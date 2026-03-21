package co.kr.woojjam.concurrency.repository.match;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.kr.woojjam.concurrency.entity.match.MatchParticipant;
import co.kr.woojjam.concurrency.entity.match.type.ParticipantStatus;

public interface MatchParticipantRepository extends JpaRepository<MatchParticipant, Long> {

	List<MatchParticipant> findAllByMatchId(final Long matchId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT mp FROM MatchParticipant mp WHERE mp.matchId = :matchId")
	List<MatchParticipant> findAllByMatchIdWithPessimisticLock(@Param("matchId") final Long matchId);
	Optional<MatchParticipant> findByMatchIdAndUserId(final Long matchId, final Long userId);

	List<MatchParticipant> findAllByMatchIdAndUserId(final Long matchId, final Long userId);

	long countByMatchIdAndStatus(final Long matchId, final ParticipantStatus status);
}
