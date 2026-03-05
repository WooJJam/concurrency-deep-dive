package co.kr.woojjam.concurrency.coupon;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;

import co.kr.woojjam.concurrency.config.TestDataBaseConfig;
import co.kr.woojjam.concurrency.entity.TestCoupon;
import co.kr.woojjam.concurrency.entity.TestUser;
import co.kr.woojjam.concurrency.repository.TestCouponRepository;
import co.kr.woojjam.concurrency.repository.TestHistoryRepository;
import co.kr.woojjam.concurrency.repository.TestUserRepository;
import co.kr.woojjam.concurrency.service.TestCouponService;
import co.kr.woojjam.concurrency.service.TestSynchronizedFacade;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Profile("local")
@SpringBootTest
public class TestCouponReentrantLockTest extends TestDataBaseConfig {

	private static final int COUPON_COUNT = 5;
	private static final int STOCK_PER_COUPON = 20;

	@Autowired
	private TestCouponService testCouponService;

	@Autowired
	private TestCouponRepository testCouponRepository;

	@Autowired
	private TestUserRepository testUserRepository;

	@Autowired
	private TestHistoryRepository testHistoryRepository;

	private final ReentrantLock globalLock = new ReentrantLock();
	private final ConcurrentHashMap<Long, ReentrantLock> lockMap = new ConcurrentHashMap<>();

	private List<Long> couponIds = new ArrayList<>();
	private Long userId;
	@Autowired
	private TestSynchronizedFacade testSynchronizedFacade;

	@BeforeEach
	void init() {
		testHistoryRepository.deleteAll();
		testCouponRepository.deleteAll();
		testUserRepository.deleteAll();

		couponIds.clear();
		lockMap.clear();

		for (int i = 1; i <= COUPON_COUNT; i++) {
			TestCoupon coupon = TestCoupon.builder()
				.code("C" + i)
				.stock(STOCK_PER_COUPON)
				.build();
			couponIds.add(testCouponRepository.save(coupon).getId());
		}

		TestUser user = TestUser.builder()
			.name("WooJJam")
			.build();
		userId = testUserRepository.save(user).getId();
	}

	@ParameterizedTest(name = "전역 락 - {0}명")
	@ValueSource(ints = {50, 100, 200, 300})
	@DisplayName("전역 ReentrantLock - TPS 비교")
	void executeCouponWithGlobalLock(int people) throws InterruptedException {
		executeTest(people, 10, (couponId, uid) -> {
			globalLock.lock();
			try {
				testCouponService.useCoupon(couponId, uid);
			} finally {
				globalLock.unlock();
			}
		});

		int expected = Math.min(people, COUPON_COUNT * STOCK_PER_COUPON);
		assertThat(testHistoryRepository.findAll().size()).isEqualTo(expected);
	}

	@ParameterizedTest(name = "쿠폰 ID별 락 - {0}명")
	@ValueSource(ints = {50, 100, 200, 300})
	@DisplayName("쿠폰 ID별 ReentrantLock - TPS 비교")
	void executeCouponWithLockPerCoupon(int people) throws InterruptedException {
		executeTest(people, 10, (couponId, uid) -> {
			ReentrantLock couponLock = lockMap.computeIfAbsent(couponId, id -> new ReentrantLock());
			couponLock.lock();
			try {
				testCouponService.useCoupon(couponId, uid);
			} finally {
				couponLock.unlock();
			}
		});

		int expected = Math.min(people, COUPON_COUNT * STOCK_PER_COUPON);
		assertThat(testHistoryRepository.findAll().size()).isEqualTo(expected);
	}

	private void executeTest(int people, int threadPoolSize, CouponTask task)
		throws InterruptedException {

		final CountDownLatch latch = new CountDownLatch(people);
		final ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);

		for (int i = 0; i < people; i++) {
			final Long couponId = couponIds.get(i % COUPON_COUNT); // 라운드로빈 분산
			executor.submit(() -> {
				try {
					task.execute(couponId, userId);
				} catch (Exception e) {
					log.info("error = {}", e.getMessage());
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();
		executor.shutdown();
	}

	@FunctionalInterface
	private interface CouponTask {
		void execute(Long couponId, Long userId) throws Exception;
	}
}
