package co.kr.woojjam.concurrency.facade;

import org.springframework.stereotype.Component;

import co.kr.woojjam.concurrency.repository.match.MatchLockRepository;
import co.kr.woojjam.concurrency.service.MatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchApplyFacade {

	private final MatchLockRepository matchLockRepository;
	private final MatchService matchService;

	public void joinMatchWithNamedLock(final Long matchId, final Long userId) {
		String lockName = "match_apply_" + matchId;
		try {
			Integer result = matchLockRepository.getLock(lockName, 3);
			if (result == null || result != 1) {
				throw new IllegalStateException("락 획득 실패");
			}
			matchService.joinMatchWithNamedLock(matchId, userId);
		} finally {
			matchLockRepository.releaseLock(lockName);
		}
	}
}
