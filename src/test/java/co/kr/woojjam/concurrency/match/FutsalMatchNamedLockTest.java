package co.kr.woojjam.concurrency.match;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.stream.LongStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
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
	private MatchApplyFacade matchApplyFacade;

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
		// given
		Long matchId = futsalMatch.getId();
		Long userId = user.getId();

		List<MatchParticipant> matchParticipants = LongStream.range(0, 12)
			.mapToObj(i -> MatchParticipant.builder()
				.matchId(matchId)
				.userId(i)
				.status(ParticipantStatus.CONFIRMED)
				.build())
			.toList();

		matchParticipantRepository.saveAll(matchParticipants);

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
		ConcurrencyExecutor.execute(50, 10, () -> matchApplyFacade.joinMatchWithNamedLock(matchId, userId));

		// then
		int size = matchParticipantRepository.findAllByMatchIdAndUserId(matchId, userId).size();
		assertThat(size).isEqualTo(12);
	}
}
