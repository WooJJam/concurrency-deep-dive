package co.kr.woojjam.concurrency.repository.match;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import co.kr.woojjam.concurrency.entity.match.MatchParticipant;
import co.kr.woojjam.concurrency.entity.match.type.ParticipantStatus;

public interface MatchParticipantRepository extends JpaRepository<MatchParticipant, Long> {

	List<MatchParticipant> findAllByMatchId(final Long matchId);
	Optional<MatchParticipant> findByMatchIdAndUserId(final Long matchId, final Long userId);

	List<MatchParticipant> findAllByMatchIdAndUserId(final Long matchId, final Long userId);

	long countByMatchIdAndStatus(final Long matchId, final ParticipantStatus status);
}
