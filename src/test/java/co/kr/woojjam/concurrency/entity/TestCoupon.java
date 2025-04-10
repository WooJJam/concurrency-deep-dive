package co.kr.woojjam.concurrency.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Entity
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TestCoupon {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String code; // 쿠폰 코드
	private int stock; // 남은 쿠폰 개수
	@Version
	private Integer version;

	@Builder
	public TestCoupon(final Long id, final String code, final int stock,
		final Integer version) {
		this.id = id;
		this.code = code;
		this.stock = stock;
		this.version = version;
	}

	public void use() {
		if (this.stock <= 0) {
			throw new IllegalStateException("쿠폰 재고가 부족합니다.");
		}
		this.stock -= 1;
	}

	public boolean isUseAble() {
		return this.stock > 0;
	}
}
