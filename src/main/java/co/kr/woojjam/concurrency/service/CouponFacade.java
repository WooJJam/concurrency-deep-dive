package co.kr.woojjam.concurrency.service;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import co.kr.woojjam.concurrency.entity.TestHistory;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponFacade {

	private final TestCouponService testCouponService;

	public void init() {
		testCouponService.init();
	}

	@Synchronized
	public TestHistory useCouponWithSynchronized(final Long couponId, final Long userId) {
		return testCouponService.useCoupon(couponId, userId);
	}

	public TestHistory useCouponWithOptimisticLock(final Long couponId, final Long userId) throws InterruptedException {
		while (true) {
			try {
				return testCouponService.useCouponOptimisticLock(couponId, userId);
			} catch (ObjectOptimisticLockingFailureException e) {
				try {
					log.info("10ms 대기");
					Thread.sleep(10); // 잠시 대기
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					throw new RuntimeException("Thread interrupted", ie);
				}
			} catch (IllegalStateException e) {
				log.error("쿠폰 소진 → 종료: {}", e.getMessage());
				if (e.getMessage().contains("쿠폰 재고가 부족")) {
					log.warn("✅ 재고가 실제로 소진됨 → 루프 종료");
					break;
				}
				Thread.sleep(10);
			}
		}
		return null;
	}
}
