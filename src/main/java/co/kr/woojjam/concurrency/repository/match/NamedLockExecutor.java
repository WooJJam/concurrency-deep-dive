package co.kr.woojjam.concurrency.repository.match;

import org.springframework.stereotype.Component;

import co.kr.woojjam.concurrency.common.lock.DistributedLockRepository;
import co.kr.woojjam.concurrency.common.lock.named.NamedLockOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class NamedLockExecutor {

	private final DistributedLockRepository<NamedLockOptions> lockRepository;

	public void executeWithLock(final NamedLockOptions namedLockOptions, final Runnable action) {
		lockRepository.withLock(namedLockOptions, action);
	}
}
