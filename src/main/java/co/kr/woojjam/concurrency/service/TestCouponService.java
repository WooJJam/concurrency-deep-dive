package co.kr.woojjam.concurrency.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import co.kr.woojjam.concurrency.entity.TestCoupon;
import co.kr.woojjam.concurrency.entity.TestHistory;
import co.kr.woojjam.concurrency.entity.TestUser;
import co.kr.woojjam.concurrency.repository.TestCouponRepository;
import co.kr.woojjam.concurrency.repository.TestHistoryRepository;
import co.kr.woojjam.concurrency.repository.TestUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestCouponService {

	private final TestCouponRepository testCouponRepository;
	private final TestUserRepository testUserRepository;
	private final TestHistoryRepository testHistoryRepository;

	/**
	 * @description
	 * User가 Coupon을 사용할 경우 수량을 감소시키고,
	 * History에 이력을 저장합니다.
	 * 아래 코드는 데드락이 발생할 가능성이 높습니다.
	 * @author woojjam
	 * @date 2025-03-12
	 **/
	@Transactional
	public TestHistory useCoupon(final Long couponId, final Long userId) {
		TestCoupon coupon = testCouponRepository.findById(couponId)
			.orElseThrow(() -> new IllegalArgumentException("쿠폰이 존재하지 않습니다."));

		TestUser user = testUserRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));

		coupon.use();

		TestHistory history = TestHistory.builder()
			.testCoupon(coupon)
			.testUser(user)
			.build();

		return testHistoryRepository.save(history);
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
	public TestHistory useCouponWithPessimisticLock(final Long couponId, final Long userId) {
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

		return history;
	}

	@Transactional
	public TestHistory useCouponOptimisticLock(final Long couponId, final Long userId) {

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

		return history;

	}

	public TestCoupon read(Long id) {
		return testCouponRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("해당 쿠폰을 찾을 수 없습니다"));
	}

	public void init() {
		TestCoupon testCoupon = TestCoupon.builder()
			.code("A")
			.stock(20)
			.build();

		TestUser user = TestUser.builder()
			.name("WooJJam")
			.build();

		testUserRepository.save(user);
		testCouponRepository.save(testCoupon);
	}
}
