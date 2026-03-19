package co.kr.woojjam.concurrency.match;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.LongStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import co.kr.woojjam.concurrency.config.TestDataBaseConfig;
import co.kr.woojjam.concurrency.entity.TestUser;
import co.kr.woojjam.concurrency.entity.match.FutsalMatch;
import co.kr.woojjam.concurrency.entity.match.MatchParticipant;
import co.kr.woojjam.concurrency.entity.match.type.ParticipantStatus;
import co.kr.woojjam.concurrency.repository.TestUserRepository;
import co.kr.woojjam.concurrency.repository.match.MatchParticipantRepository;
import co.kr.woojjam.concurrency.repository.match.MatchRepository;
import co.kr.woojjam.concurrency.service.MatchService;

@DataJpaTest
@Import(MatchService.class)
public class FutsalMatchNamedLockTest extends TestDataBaseConfig {

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

	/*
	시나리오 1: 단일 유저가 매치에 신청하면 PENDING 상태로 참가자가 등록된다
	시나리오 2: 정원이 가득 찬 매치에 신청하면 실패한다
	시나리오 3: 50명 동시성 테스트
	시나리오 4: 300명이 동시에 12명 정원 매치에 신청하면 정확히 12명만 참가 확정된다

	 */

	@Test
	@DisplayName("단일 유저가 매치에 신청하면 결제 전 PENDING 상태로 참가자가 등록된다")
	void 단일_유저_매치_성공() {
		// given
		Long matchId = futsalMatch.getId();
		Long userId = user.getId();

		// when
		matchService.joinMatch(matchId, userId);

		// then
		MatchParticipant matchParticipant = matchParticipantRepository.findByMatchIdAndUserId(matchId, userId).get();

		assertThat(matchParticipant.getStatus()).isEqualTo(ParticipantStatus.PENDING);
		assertThat(matchParticipant.getMatchId()).isEqualTo(matchId);
		assertThat(matchParticipant.getUserId()).isEqualTo(userId);
	}

	@Test
	@DisplayName("정원이 가득 찬 매치에 신청하면 실패한다.")
	void 정원이_가득_찬_경우_신청_실패() {

		// given
		Long matchId = futsalMatch.getId();
		Long userId = user.getId();

		List<MatchParticipant> matchParticipants = LongStream.range(0, 12)
			.mapToObj((i) -> {
				return MatchParticipant.builder()
					.matchId(matchId)
					.userId(i)
					.status(ParticipantStatus.CONFIRMED)
					.build();
			}).toList();

		matchParticipantRepository.saveAll(matchParticipants);

		// when & then
		assertThatThrownBy(() -> matchService.joinMatch(matchId, userId))
			.isInstanceOf(IllegalStateException.class);
	}

	@AfterEach
	void cleanup() {
		matchParticipantRepository.deleteAll();
		matchRepository.deleteAll();
		testUserRepository.deleteAll();
	}
}
