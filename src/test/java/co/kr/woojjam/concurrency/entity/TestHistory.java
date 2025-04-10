package co.kr.woojjam.concurrency.entity;

import org.apache.catalina.User;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class TestHistory {

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	private TestUser testUser;

	@ManyToOne(fetch = FetchType.LAZY)
	private TestCoupon testCoupon;

	private String code;

	@Builder
	public TestHistory(final Long id, final TestUser testUser, final TestCoupon testCoupon, final String code) {
		this.id = id;
		this.testCoupon = testCoupon;
		this.testUser = testUser;
		this.code = code;
	}
}
