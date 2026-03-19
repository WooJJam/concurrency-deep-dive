package co.kr.woojjam.concurrency.repository.match;

import org.springframework.data.jpa.repository.JpaRepository;

import co.kr.woojjam.concurrency.entity.match.FutsalMatch;

public interface MatchRepository extends JpaRepository<FutsalMatch, Long> {
}
