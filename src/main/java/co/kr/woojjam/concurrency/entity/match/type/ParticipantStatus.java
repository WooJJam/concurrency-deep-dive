package co.kr.woojjam.concurrency.entity.match.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ParticipantStatus {

	PENDING("결제 대기중"),
	CONFIRMED("결제 완료"),
	CANCELLED("결제 취소");

	private final String name;
}
