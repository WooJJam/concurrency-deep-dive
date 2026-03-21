package co.kr.woojjam.concurrency.repository.match;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import co.kr.woojjam.concurrency.entity.match.FutsalMatch;
import jakarta.persistence.LockModeType;

public interface MatchRepository extends JpaRepository<FutsalMatch, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT m FROM FutsalMatch m WHERE m.id = :id")
	Optional<FutsalMatch> findByIdWithPessimisticLock(Long id);
}
