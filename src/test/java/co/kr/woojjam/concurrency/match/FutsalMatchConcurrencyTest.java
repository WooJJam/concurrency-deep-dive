package co.kr.woojjam.concurrency.match;

import static org.assertj.core.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;

import co.kr.woojjam.concurrency.config.TestDataBaseConfig;
import co.kr.woojjam.concurrency.entity.TestUser;
import co.kr.woojjam.concurrency.entity.match.FutsalMatch;
import co.kr.woojjam.concurrency.repository.TestUserRepository;
import co.kr.woojjam.concurrency.repository.match.MatchParticipantRepository;
import co.kr.woojjam.concurrency.repository.match.MatchRepository;
import co.kr.woojjam.concurrency.service.MatchService;

@Profile("local")
@SpringBootTest
public class FutsalMatchConcurrencyTest extends TestDataBaseConfig {

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

	private void executeTest(int people, int threadPoolSize, MatchTask task) throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(people);
		ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);

		for (long i = 0; i < people; i++) {
			executor.submit(() -> {
				try {
					task.execute();
				} catch (Exception e) {
					// 정원 초과 예외는 무시
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
