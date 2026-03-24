package co.kr.woojjam.concurrency.facade;

import org.springframework.stereotype.Component;

import co.kr.woojjam.concurrency.common.lock.named.NamedLockOptions;
import co.kr.woojjam.concurrency.repository.match.NamedLockExecutor;
import co.kr.woojjam.concurrency.service.MatchService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MatchApplyFacade {

	private final MatchService matchService;
	private final NamedLockExecutor namedLockExecutor;

	public void joinMatchWithNamedLock(final Long matchId, final Long userId) {

		NamedLockOptions namedLockOptions = new NamedLockOptions("match_apply_" + matchId, 3);
		namedLockExecutor.executeWithLock(namedLockOptions, () -> matchService.joinMatchWithNamedLock(matchId, userId));
	}
}
