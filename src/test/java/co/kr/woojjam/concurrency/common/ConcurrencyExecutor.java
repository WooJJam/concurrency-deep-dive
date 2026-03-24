package co.kr.woojjam.concurrency.common;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;


public class ConcurrencyExecutor {

	public static void execute(int people, int threadPoolSize, Task task) throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(people);
		ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
		AtomicInteger rollbackCount = new AtomicInteger(0);

		for (long i = 0; i < people; i++) {
			executor.submit(() -> {
				try {
					task.execute();
				} catch (Exception e) {
					rollbackCount.incrementAndGet();
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();
		executor.shutdown();
	}

	@FunctionalInterface
	public interface Task {
		void execute() throws Exception;
	}

}
