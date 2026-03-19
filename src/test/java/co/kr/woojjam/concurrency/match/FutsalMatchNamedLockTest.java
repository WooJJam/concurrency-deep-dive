package co.kr.woojjam.concurrency.match;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

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

	@Test
	@DisplayName("단일 유저가 매치에 신청하면 결제 전 PENDING 상태로 참가자가 등록된다")
	void applyMatch() {
		// given
		Long matchId = futsalMatch.getId();
		Long userId = user.getId();

		// when
		matchService.joinMatch(matchId, userId);

		// then
		MatchParticipant matchParticipant = matchParticipantRepository.findAllByMatchIdAndUserId(matchId, userId).get();

		assertThat(matchParticipant.getStatus()).isEqualTo(ParticipantStatus.PENDING);
		assertThat(matchParticipant.getMatchId()).isEqualTo(matchId);
		assertThat(matchParticipant.getUserId()).isEqualTo(userId);
	}
}
