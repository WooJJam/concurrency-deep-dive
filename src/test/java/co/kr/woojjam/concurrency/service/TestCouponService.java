package co.kr.woojjam.concurrency.service;

import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import co.kr.woojjam.concurrency.entity.TestCoupon;
import co.kr.woojjam.concurrency.entity.TestHistory;
import co.kr.woojjam.concurrency.entity.TestUser;
import co.kr.woojjam.concurrency.repository.TestCouponRepository;
import co.kr.woojjam.concurrency.repository.TestHistoryRepository;
import co.kr.woojjam.concurrency.repository.TestUserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestCouponService {

	private final TestCouponRepository testCouponRepository;
	private final TestUserRepository testUserRepository;
	private final TestHistoryRepository testHistoryRepository;
	@Autowired
	private EntityManagerFactory emf;

	/**
	 * @description
	 * User가 Coupon을 사용할 경우 수량을 감소시키고,
	 * History에 이력을 저장합니다.
	 * 아래 코드는 데드락이 발생할 가능성이 높습니다.
	 * @author woojjam
	 * @date 2025-03-12
	 **/
	@Transactional
	public void useCoupon(final Long couponId, final Long userId) {
		TestCoupon coupon = testCouponRepository.findById(couponId)
			.orElseThrow(() -> new IllegalArgumentException("쿠폰이 존재하지 않습니다."));

		TestUser user = testUserRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));

		coupon.use();

		TestHistory history = TestHistory.builder()
			.testCoupon(coupon)
			.testUser(user)
			.build();

		testHistoryRepository.save(history);
	}

	@Transactional(isolation = Isolation.SERIALIZABLE)
	public void useCouponWithIsolationLevel(final Long couponId, final Long userId) {
		TestCoupon coupon = testCouponRepository.findById(couponId)
			.orElseThrow(() -> new IllegalArgumentException("쿠폰이 존재하지 않습니다."));

		TestUser user = testUserRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));

		coupon.use();

		TestHistory history = TestHistory.builder()
			.testCoupon(coupon)
			.testUser(user)
			.build();

		testHistoryRepository.save(history);
	}

	@Transactional
	public void useCouponWithPessimisticLock(final Long couponId, final Long userId) {
		TestCoupon coupon = testCouponRepository.findByIdWithPessimisticWrite(couponId)
			.orElseThrow(() -> new IllegalArgumentException("쿠폰이 존재하지 않습니다."));

		TestUser user = testUserRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));

		coupon.use();

		TestHistory history = TestHistory.builder()
			.testCoupon(coupon)
			.testUser(user)
			.build();

		testHistoryRepository.save(history);
	}

	@Transactional
	public void useCouponOptimisticLock(final Long couponId, final Long userId) {

		TestCoupon coupon = testCouponRepository.findById(1L).orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));

		log.info("coupon = {}, version = {}", coupon.getStock(), coupon.getVersion());

		coupon.use();

		TestUser user = testUserRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));

		TestHistory history = TestHistory.builder()
			.testCoupon(coupon)
			.testUser(user)
			.build();

		testHistoryRepository.save(history);
	}

	@Transactional
	public void useCouponOptimisticLockWithExplicitLocking(final Long couponId, final Long userId) throws
		InterruptedException {

		TestCoupon coupon = testCouponRepository.findByWithOptimistic(1L).orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));
		coupon.use();

		Thread.sleep(1000);
		log.info("----------- 1초 대기 -----------");
		TestUser user = testUserRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));

		TestHistory history = TestHistory.builder()
			.code(coupon.getCode())
			.testUser(user)
			.build();

		testHistoryRepository.save(history);
	}

	@Transactional
	public void txA() {
		TestCoupon coupon = testCouponRepository.findByIdWithPessimisticWrite(1L).orElseThrow();

		TestUser user = testUserRepository.findByIdWithPessimisticWrite(1L).orElseThrow();

		coupon.use();

		TestHistory history = TestHistory.builder()
			.code(coupon.getCode())
			.testUser(user)
			.build();

		testHistoryRepository.save(history);
	}

	@Transactional
	public void txB() {
		TestUser user = testUserRepository.findByIdWithPessimisticWrite(1L).orElseThrow();

		TestCoupon coupon = testCouponRepository.findByIdWithPessimisticWrite(1L).orElseThrow();

		coupon.use();

		TestHistory history = TestHistory.builder()
			.code(coupon.getCode())
			.testUser(user)
			.build();

		testHistoryRepository.save(history);
	}



	public TestCoupon read(Long id) {
		return testCouponRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("해당 쿠폰을 찾을 수 없습니다"));
	}
}
