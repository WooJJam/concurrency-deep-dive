package co.kr.woojjam.concurrency.entity.match;

import org.apache.catalina.User;

import co.kr.woojjam.concurrency.entity.TestUser;
import co.kr.woojjam.concurrency.entity.match.type.ParticipantStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class MatchParticipant {

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long matchId;

	private Long userId;

	@Enumerated(EnumType.ORDINAL)
	private ParticipantStatus status;

	@Builder
	public MatchParticipant(final Long id, final Long matchId, final ParticipantStatus status, final Long userId) {
		this.id = id;
		this.matchId = matchId;
		this.status = status;
		this.userId = userId;
	}
}
