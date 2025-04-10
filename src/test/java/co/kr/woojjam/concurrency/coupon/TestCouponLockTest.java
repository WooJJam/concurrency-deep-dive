package co.kr.woojjam.concurrency.coupon;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import co.kr.woojjam.concurrency.entity.TestCoupon;
import co.kr.woojjam.concurrency.entity.TestHistory;
import co.kr.woojjam.concurrency.entity.TestUser;
import co.kr.woojjam.concurrency.repository.TestCouponRepository;
import co.kr.woojjam.concurrency.repository.TestHistoryRepository;
import co.kr.woojjam.concurrency.repository.TestUserRepository;
import co.kr.woojjam.concurrency.service.SynchronizedFacade;
import co.kr.woojjam.concurrency.service.TestCouponService;
import jakarta.persistence.OptimisticLockException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Profile("local")
@SpringBootTest
public class TestCouponLockTest {
	//
	@Autowired
	private TestCouponService testCouponService;
	@Autowired
	private SynchronizedFacade synchronizedFacade;
	@Autowired
	private TestCouponRepository testCouponRepository;
	@Autowired
	private TestHistoryRepository testHistoryRepository;

	@Autowired
	private TestUserRepository testUserRepository;

	private TestCoupon testCoupon;
	private TestUser testUser;

	@BeforeEach
	void init() {
		testCoupon = TestCoupon.builder()
			.code("A")
			.stock(20)
			.build();

		TestUser user = TestUser.builder()
			.name("WooJJam")
			.build();

		testUserRepository.save(user);
		testCouponRepository.save(testCoupon);
	}

	@Test
	@DisplayName("100명의 유저가 A 쿠폰 20장을 동시에 사용할려고 경쟁하는 상황")
	void executeCoupon() throws InterruptedException {

		final int people = 100;
		final Long couponId = 1L;
		final Long userId = 1L;
		final CountDownLatch countDownLatch = new CountDownLatch(people);

		List<Thread> workers = Stream
			.generate(() -> new Thread(new LockWorker(couponId, userId, countDownLatch)))
			.limit(people)
			.toList();

		workers.forEach(Thread::start);
		countDownLatch.await();

		List<TestHistory> results = testHistoryRepository.findAll();

		assertThat(results.size()).isEqualTo(20);

	}

	@Test
	@DisplayName("격리 수준(Serializable)으로 동시성 제어하기")
	void executeCouponWithIsolationLevel() throws InterruptedException {

		final int people = 100;
		final Long couponId = 1L;
		final Long userId = 1L;
		final CountDownLatch countDownLatch = new CountDownLatch(people);

		List<Thread> workers = Stream
			.generate(() -> new Thread(new LockWorker(couponId, userId, countDownLatch)))
			.limit(people)
			.toList();

		workers.forEach(Thread::start);
		countDownLatch.await();

		List<TestHistory> results = testHistoryRepository.findAll();

		assertThat(results.size()).isEqualTo(20);

	}

	@Test
	@DisplayName("Synchronized로 동시성 제어하기")
	void executeCouponWithSynchronized() throws InterruptedException {

		final int people = 100;
		final Long couponId = 1L;
		final Long userId = 1L;
		final CountDownLatch countDownLatch = new CountDownLatch(people);

		List<Thread> workers = Stream
			.generate(() -> new Thread(new LockWorker(couponId, userId, countDownLatch)))
			.limit(people)
			.toList();

		workers.forEach(Thread::start);
		countDownLatch.await();

		List<TestHistory> results = testHistoryRepository.findAll();

		assertThat(results.size()).isEqualTo(20);

	}

	@Test
	@DisplayName("낙관적으로 동시성 제어하기")
	void executeCouponWithOptimisticLock() throws InterruptedException {
		final int people = 300;
		final Long couponId = 1L;
		final Long userId = 1L;
		final CountDownLatch countDownLatch = new CountDownLatch(people);

		List<Thread> workers = Stream
			.generate(() -> new Thread(new LockWorker(couponId, userId, countDownLatch)))
			.limit(people)
			.toList();

		long start = System.currentTimeMillis();

		workers.forEach(Thread::start);
		countDownLatch.await();

		long end = System.currentTimeMillis();
		long duration = end - start;

		System.out.println("===================================================");
		System.out.println("📌 Optimistic Lock Total execution time (ms): " + duration);
		System.out.println("===================================================");

		List<TestHistory> results = testHistoryRepository.findAll();
		assertThat(results.size()).isEqualTo(20);
	}

	@Test
	@DisplayName("비관적으로 동시성 제어하기")
	void executeCouponWithPessimisticLock() throws InterruptedException {
		final int people = 300;
		final Long couponId = 1L;
		final Long userId = 1L;
		final CountDownLatch countDownLatch = new CountDownLatch(people);

		List<Thread> workers = Stream
			.generate(() -> new Thread(new LockWorker(couponId, userId, countDownLatch)))
			.limit(people)
			.toList();

		long start = System.currentTimeMillis();

		workers.forEach(Thread::start);
		countDownLatch.await();

		long end = System.currentTimeMillis();
		long duration = end - start;

		System.out.println("===================================================");
		System.out.println("📌 Pessimistic Lock Total execution time (ms): " + duration);
		System.out.println("===================================================");


		List<TestHistory> results = testHistoryRepository.findAll();
		assertThat(results.size()).isEqualTo(20);
	}

	@Test
	void testOptimisticLockReadTwiceVersionChange() throws InterruptedException {
		Long couponId = 1L;

		CountDownLatch readyLatch = new CountDownLatch(2);  // A, B 준비 신호
		CountDownLatch startLatch = new CountDownLatch(1);  // 동시에 출발
		CountDownLatch doneLatch = new CountDownLatch(2);   // 완료 대기

		Thread threadA = new Thread(() -> {
			try {
				readyLatch.countDown();         // 준비됨
				startLatch.await();             // 출발 대기
				testCouponService.useCouponOptimisticLock(couponId, 1L);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} finally {
				doneLatch.countDown();
			}
		});

		Thread threadB = new Thread(() -> {
			try {
				readyLatch.countDown();         // 준비됨
				startLatch.await();             // 출발 대기
				testCouponService.useCouponOptimisticLockWithExplicitLocking(couponId, 1L);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} finally {
				doneLatch.countDown();
			}
		});

		threadA.start();
		threadB.start();

		readyLatch.await();

		startLatch.countDown();

		doneLatch.await();
	}

	@Test
	@DisplayName("비관적 락 데드락 테스트")
	void testPessimisticLockCausedByDeadLock() throws InterruptedException {
		ExecutorService executor = Executors.newFixedThreadPool(2);

		executor.submit(() -> {
			try {
				testCouponService.txA();
			} catch (Exception e) {
				log.error("xA failed: " + e.getMessage());
				e.getCause().printStackTrace();
			}
		});

		executor.submit(() -> {
			try {
				testCouponService.txB();
			} catch (Exception e) {
				log.error("txB failed: " + e.getMessage());
				e.getCause().printStackTrace();
			}
		});

		executor.shutdown(); // 작업 제출 종료

		// 최대 5초 대기
		boolean finished = executor.awaitTermination(5, TimeUnit.SECONDS);
		if (!finished) {
			log.error("데드락 발생");
		}
	}

	private class LockWorker implements Runnable {

		private Long couponId;
		private Long userId;
		private CountDownLatch countDownLatch;

		public LockWorker(final Long couponId, final Long userId, final CountDownLatch countDownLatch) {
			this.couponId = couponId;
			this.userId = userId;
			this.countDownLatch = countDownLatch;
		}

		@Override
		public void run() {
			try {
				// testCouponService.useCoupon(couponId, userId);
				// testCouponService.useCouponWithIsolationLevel(couponId, userId);
				// synchronizedFacade.useCouponWithSynchronized(couponId, userId);
				// testCouponService.useCouponWithPessimisticLock(couponId, userId);
				runOptimisticLock(countDownLatch);

			} catch (Exception e) {
				log.info("error = {}", e.getMessage());
				Thread.currentThread().interrupt();
			} finally {
				countDownLatch.countDown();
			}
		}
	}

	private void runOptimisticLock(CountDownLatch countDownLatch) {
		while (true) {
			try {
				testCouponService.useCouponOptimisticLock(1L, 1L);
				break;
			} catch (ObjectOptimisticLockingFailureException e) {
				log.info("10ms 대기");
			} catch (IllegalStateException e) {
				log.error("쿠폰 소진 → 종료: {}", e.getMessage());
				break;
			} catch (Exception e) {
				log.error("예상하지 못한 예외 발생: {}", e.getClass().getSimpleName());
				break;
			}
		}
	}
}
