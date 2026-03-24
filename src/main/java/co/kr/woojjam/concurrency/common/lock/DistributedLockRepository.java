package co.kr.woojjam.concurrency.common.lock;

public interface DistributedLockRepository<O extends LockOptions> {

	void withLock(O options, Runnable action);
}
