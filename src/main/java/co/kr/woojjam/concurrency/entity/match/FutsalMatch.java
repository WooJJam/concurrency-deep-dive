package co.kr.woojjam.concurrency.entity.match;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "futsal_match")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FutsalMatch {

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String name;
	private int participants;
	private int currentCount;

	// @Version
	// private Long version;

	@Builder
	public FutsalMatch(final Long id, final String name, final int participants) {
		this.id = id;
		this.name = name;
		this.participants = participants;
	}

	public boolean isApply(final int count) {
		if (count >= this.participants) {
			throw new IllegalStateException("해당 매치는 마감되었습니다.");
		}

		return true;
	}

	public void increaseCount() {
		if (this.currentCount >= this.participants) {
			throw new IllegalStateException("해당 매치는 마감되었습니다.");
		}
		this.currentCount++;
	}
}
