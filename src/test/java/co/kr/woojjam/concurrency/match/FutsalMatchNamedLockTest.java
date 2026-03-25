package co.kr.woojjam.concurrency.match;

import static org.assertj.core.api.Assertions.*;

import java.sql.Connection;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.redis.testcontainers.RedisContainer;

import co.kr.woojjam.concurrency.common.ConcurrencyExecutor;
import co.kr.woojjam.concurrency.entity.TestUser;
import co.kr.woojjam.concurrency.entity.match.FutsalMatch;
import co.kr.woojjam.concurrency.entity.match.MatchParticipant;
import co.kr.woojjam.concurrency.entity.match.type.ParticipantStatus;
import co.kr.woojjam.concurrency.facade.MatchApplyFacade;
import co.kr.woojjam.concurrency.repository.TestUserRepository;
import co.kr.woojjam.concurrency.repository.match.MatchParticipantRepository;
import co.kr.woojjam.concurrency.repository.match.MatchRepository;
import co.kr.woojjam.concurrency.service.MatchService;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
public class FutsalMatchNamedLockTest {

	@Container
	static MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0.35"));

	@Container
	static RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7.2.4-alpine"));

	@DynamicPropertySource
	static void setProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", mysql::getJdbcUrl);
		registry.add("spring.datasource.username", mysql::getUsername);
		registry.add("spring.datasource.password", mysql::getPassword);
		registry.add("spring.lock-datasource.url", mysql::getJdbcUrl);
		registry.add("spring.lock-datasource.username", mysql::getUsername);
		registry.add("spring.lock-datasource.password", mysql::getPassword);
		registry.add("spring.data.redis.host", redis::getHost);
		registry.add("spring.data.redis.port", () -> String.valueOf(redis.getMappedPort(6379)));
	}

	@Autowired
	private DataSource dataSource;

	@Autowired
	private MatchApplyFacade matchApplyFacade;

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
	@DisplayName("단일 유저가 네임드 락으로 매치에 신청하면 PENDING 상태로 참가자가 등록된다")
	void 단일_유저_매치_성공() {
		// given
		Long matchId = futsalMatch.getId();
		Long userId = user.getId();

		// when
		matchApplyFacade.joinMatchWithNamedLock(matchId, userId);

		// then
		MatchParticipant matchParticipant = matchParticipantRepository.findByMatchIdAndUserId(matchId, userId).get();

		assertThat(matchParticipant.getStatus()).isEqualTo(ParticipantStatus.PENDING);
		assertThat(matchParticipant.getMatchId()).isEqualTo(matchId);
		assertThat(matchParticipant.getUserId()).isEqualTo(userId);
	}

	@Test
	@DisplayName("정원이 가득 찬 매치에 네임드 락으로 신청하면 실패한다")
	void 정원이_가득_찬_경우_신청_실패() {
		// given - 서비스를 통해 12명을 등록하여 currentCount를 정확히 채움
		Long matchId = futsalMatch.getId();
		Long userId = user.getId();

		for (long i = 0; i < 12; i++) {
			matchService.joinMatchWithNamedLock(matchId, i);
		}

		// when & then
		assertThatThrownBy(() -> matchApplyFacade.joinMatchWithNamedLock(matchId, userId))
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	@DisplayName("네임드 락으로 50명이 동시에 신청하면 정확히 12명만 등록된다")
	void 네임드_락_동시성_테스트() throws InterruptedException {
		// given
		Long matchId = futsalMatch.getId();
		Long userId = user.getId();

		// when
		ConcurrencyExecutor.execute(50, 19, () -> matchApplyFacade.joinMatchWithNamedLock(matchId, userId));

		// then
		int size = matchParticipantRepository.findAllByMatchIdAndUserId(matchId, userId).size();
		assertThat(size).isEqualTo(12);
	}


	@Nested
	class NamedLockProblem1 {
		@Test
		@Transactional
		@DisplayName("문제1-1: 같은 커넥션(세션)에서 GET_LOCK을 두 번 호출하면 재진입으로 두 번째도 성공한다")
		void 문제1_재진입() {
			String lockName = "reentrance_test";

			// @Transactional로 conn1을 트랜잭션 전체에 바인딩 → 두 쿼리 모두 같은 커넥션 사용 강제
			// 첫 번째 GET_LOCK: conn1 세션이 락 획득
			Integer first = matchRepository.getNamedLockByNativeQuery(lockName, 0);

			// 두 번째 GET_LOCK: 같은 conn1 세션 → MySQL Named Lock 재진입 → 획득 성공
			Integer second = matchRepository.getNamedLockByNativeQuery(lockName, 0);

			assertThat(first).isEqualTo(1);
			assertThat(second).isEqualTo(1); // 락이 있어도 같은 세션이면 통과 (문제의 핵심)
		}

		@Test
		@DisplayName("문제1-2: GET_LOCK과 RELEASE_LOCK이 다른 커넥션에서 실행되면 락이 해제되지 않는다")
		void 문제1_RELEASE_LOCK_다른_커넥션() throws Exception {
			String lockName = "release_mismatch_test";

			// GET_LOCK: conn1 사용 후 즉시 풀에 반납 (lock은 conn1 세션에 귀속)
			Integer getLockResult = matchRepository.getNamedLockByNativeQuery(lockName, 0);
			assertThat(getLockResult).isEqualTo(1);

			// conn1을 수동으로 점유 → RELEASE_LOCK이 다른 커넥션(conn2)을 사용하도록 강제
			Connection conn1 = dataSource.getConnection();
			try {
				// RELEASE_LOCK: conn2 세션은 lockName 락을 보유하지 않음 → 해제 실패(0)
				Integer releaseResult = matchRepository.releaseNamedLockByNativeQuery(lockName);
				assertThat(releaseResult).isEqualTo(0); // 다른 커넥션 → 락 해제 실패
			} finally {
				conn1.close(); // conn1 반납 시 MySQL 세션 종료 → 락 자동 해제
			}
		}
	}

	@Test
	@DisplayName("@Transactional과 네임드 락을 같이 사용할 경우 12명이 초과된다.")
	void 문제2_Transactional_네임드_락_동시성_테스트() throws InterruptedException {
		// given
		Long matchId = futsalMatch.getId();
		Long userId = user.getId();

		// when
		ConcurrencyExecutor.execute(50, 10, () -> matchApplyFacade.joinMatchWithNamedLockTransactional(matchId, userId));

		// then
		int size = matchParticipantRepository.findAllByMatchIdAndUserId(matchId, userId).size();
		assertThat(size).isEqualTo(12);
	}

}
