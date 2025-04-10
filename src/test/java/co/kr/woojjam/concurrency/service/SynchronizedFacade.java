package co.kr.woojjam.concurrency.service;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.Synchronized;

@Component
@RequiredArgsConstructor
public class SynchronizedFacade {

	private final TestCouponService testCouponService;

	@Synchronized
	public void useCouponWithSynchronized(final Long couponId, final Long userId) {
		testCouponService.useCoupon(couponId, userId);
	}
}
