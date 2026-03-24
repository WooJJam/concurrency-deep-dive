package co.kr.woojjam.concurrency.common.lock.named;

import co.kr.woojjam.concurrency.common.lock.LockOptions;

public record NamedLockOptions(String key, int timeoutSeconds) implements LockOptions {
}
