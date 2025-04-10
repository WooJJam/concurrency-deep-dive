package co.kr.woojjam.concurrency.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.kr.woojjam.concurrency.entity.TestHistory;
import co.kr.woojjam.concurrency.service.CouponFacade;
import co.kr.woojjam.concurrency.service.TestCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/coupons")
public class TestController {

	private final CouponFacade couponFacade;
	private final TestCouponService testCouponService;

	@PostMapping("/init")
	public void init() {
		couponFacade.init();
	}

	@PostMapping("/use")
	public ResponseEntity<?> useCoupon() {
		log.info("쿠폰을 사용합니다.");
		TestHistory history = couponFacade.useCouponWithSynchronized(1L, 1L);
		return ResponseEntity.ok().body(history);
	}

	@PostMapping("/pessimistic-lock")
	public ResponseEntity<?> useCouponPessimisticLock() {
		log.info("쿠폰을 사용합니다.");
		TestHistory history = testCouponService.useCouponWithPessimisticLock(1L, 1L);
		return ResponseEntity.ok().body(history);
	}

	@PostMapping("/optimistic-lock")
	public ResponseEntity<?> useCouponOptimisticLock() throws InterruptedException {
		log.info("쿠폰을 사용합니다.");
		TestHistory history = couponFacade.useCouponWithOptimisticLock(1L, 1L);
		return ResponseEntity.ok().body(history);
	}
}
