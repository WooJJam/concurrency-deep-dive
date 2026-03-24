package co.kr.woojjam.concurrency.match;

import static org.assertj.core.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import lombok.extern.slf4j.Slf4j;

import co.kr.woojjam.concurrency.config.TestDataBaseConfig;
import co.kr.woojjam.concurrency.entity.TestUser;
import co.kr.woojjam.concurrency.entity.match.FutsalMatch;
import co.kr.woojjam.concurrency.repository.TestUserRepository;
import co.kr.woojjam.concurrency.repository.match.MatchParticipantRepository;
import co.kr.woojjam.concurrency.repository.match.MatchRepository;
import co.kr.woojjam.concurrency.service.MatchService;

@Slf4j
@SpringBootTest
public class FutsalMatchConcurrencyTest {

	@Autowired
	private MatchService matchService;

	@Autowired
	private MatchRepository matchRepository;

	@Autowired
	private MatchParticipantRepository matchParticipantRepository;

	@Autowired
	private TestUserRepository testUserRepository;

	private FutsalMatch futsalMatch;
	private TestUser user;

	@BeforeEach
	void init() {
		futsalMatch = matchRepository.save(FutsalMatch.builder()
			.name("풋살 매치")
			.participants(12)
			.build());

		user = testUserRepository.save(TestUser.builder()
			.name("홍길동")
			.build());
	}

	@AfterEach
	void cleanup() {
		matchParticipantRepository.deleteAll();
		matchRepository.deleteAll();
		testUserRepository.deleteAll();
	}

	@Test
	@DisplayName("동시성 제어 없이 50명이 신청하면 정원 12명을 초과하여 등록된다")
	void 동시성_제어_없이_정원_초과_발생() throws InterruptedException {
		// given
		init();

		Long matchId = futsalMatch.getId();
		Long userId = user.getId();

		// when
		executeTest(50, 10, () -> matchService.joinMatch(matchId, userId));

		// then - 네임드락 적용 전: 동시성 문제로 12개 초과 발생
		int size = matchParticipantRepository.findAllByMatchIdAndUserId(matchId, userId).size();
		assertThat(size).isGreaterThan(12);
	}

	@Test
	@DisplayName("비관적 락을 사용하여 50명이 매치에 신청하면 12명이 등록된다. (Gap Lock - 데드락 발생)")
	void 비관적_락_적용() throws InterruptedException {
		// given
		Long matchId = futsalMatch.getId();
		Long userId = user.getId();

		// when
		executeTest(50, 10, () -> matchService.joinMatchWithPessimisticLock(matchId, userId));

		// then - 네임드락 적용 전: 동시성 문제로 12개 초과 발생
		int size = matchParticipantRepository.findAllByMatchIdAndUserId(matchId, userId).size();
		assertThat(size).isEqualTo(12);
	}

	@Test
	@DisplayName("비관적 락과 Gap Lock으로 50명이 매치에 신청하면 12명이 등록된다. (데드락 발생)")
	void 비관적_락과_GAP_Lock_적용() throws InterruptedException {
		// given
		Long matchId = futsalMatch.getId();
		Long userId = user.getId();

		// when
		executeTest(50, 10, () -> matchService.joinMatchWithPessimisticLockAndGapLock(matchId, userId));

		// then - 네임드락 적용 전: 동시성 문제로 12개 초과 발생
		int size = matchParticipantRepository.findAllByMatchIdAndUserId(matchId, userId).size();
		assertThat(size).isEqualTo(12);
	}

	@Test
	@DisplayName("낙관적 락 적용 시 재시도 없으면 12명 미만이 등록된다 (버전 충돌로 인한 손실)")
	void 낙관적_락_재시도_없이_정원_미달() throws InterruptedException {
		// given
		Long matchId = futsalMatch.getId();
		Long userId = user.getId();

		// when
		executeTest(50, 10, () -> matchService.joinMatchWithOptimisticLock(matchId, userId));

		// then - 충돌로 롤백된 요청은 재시도 없이 소멸 → 12명 미달
		int size = matchParticipantRepository.findAllByMatchIdAndUserId(matchId, userId).size();
		assertThat(size).isLessThan(12);
	}

	@Test
	@DisplayName("낙관적 락에 재시도를 적용하면 정확히 12명이 등록된다")
	void 낙관적_락_재시도_적용() throws InterruptedException {
		// given
		Long matchId = futsalMatch.getId();
		Long userId = user.getId();

		// when - 버전 충돌 시 재시도, 정원 초과 시 종료
		executeTest(50, 10, () -> {
			while (true) {
				try {
					matchService.joinMatchWithOptimisticLock(matchId, userId);
					return;
				} catch (ObjectOptimisticLockingFailureException e) {
					log.info("동시성 충돌 발생");
					Thread.sleep(10);
				} catch (IllegalStateException e) {
					return; // 정원 초과 → 재시도 불필요
				}
			}
		});

		// then
		int size = matchParticipantRepository.findAllByMatchIdAndUserId(matchId, userId).size();
		int currentCount = matchRepository.findById(matchId).get().getCurrentCount();
		assertThat(size).isEqualTo(12);
		assertThat(currentCount).isEqualTo(12);
	}

	private void executeTest(int people, int threadPoolSize, MatchTask task) throws InterruptedException {
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
	private interface MatchTask {
		void execute() throws Exception;
	}
}
