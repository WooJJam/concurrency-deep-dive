package co.kr.woojjam.concurrency.client;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PaymentClient {

	public void pay() {

		try {
			log.info("결제 API 호출");
			Thread.sleep(200);
			log.info("결제 승인");
		} catch (InterruptedException e) {
			log.error("e = {}", e.getMessage(), e);
		}
	}
}
