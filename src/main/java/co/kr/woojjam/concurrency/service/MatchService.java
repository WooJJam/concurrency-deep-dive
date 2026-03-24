package co.kr.woojjam.concurrency.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.kr.woojjam.concurrency.client.PaymentClient;
import co.kr.woojjam.concurrency.entity.match.FutsalMatch;
import co.kr.woojjam.concurrency.entity.match.MatchParticipant;
import co.kr.woojjam.concurrency.entity.match.type.ParticipantStatus;
import co.kr.woojjam.concurrency.repository.TestUserRepository;
import co.kr.woojjam.concurrency.repository.match.MatchParticipantRepository;
import co.kr.woojjam.concurrency.repository.match.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchService {

	private final PaymentClient paymentClient;
	private final TestUserRepository userRepository;
	private final MatchRepository matchRepository;
	private final MatchParticipantRepository matchParticipantRepository;

	@Transactional
	public void init() {

		FutsalMatch futsalMatch = FutsalMatch.builder()
			.name("TEST 매치")
			.participants(12)
			.build();

		matchRepository.save(futsalMatch);

		// List<MatchParticipant> participants = LongStream.range(0, 11)
		// 	.mapToObj(i -> MatchParticipant.builder()
		// 		.userId(i)
		// 		.matchId(futsalMatch.getId())
		// 		.status(ParticipantStatus.PENDING)
		// 		.build())
		// 	.toList();
		//
		// log.info("participant size = {}", participants.size());

		// matchParticipantRepository.saveAll(participants);
		// matchRepository.save(futsalMatch);
	}

	@Transactional
	public void deleteAll() {
		matchParticipantRepository.deleteAll();
		matchRepository.deleteAll();
	}

	/*
  1. 매치 조회 (일반 SELECT, 락 없음)

  2. GET_LOCK('match_apply_{matchId}', 3)
  	2-1. 락 획득 실패 → 잠시 후 재시도 or 실패 반환

  3. 잔여 인원 확인 COUNT(match_participant WHERE match_id = ? AND status = 'CONFIRMED') >= max_capacity → RELEASE_LOCK 후 실패 반환

  4. 참가자 INSERT (status = 'PENDING')

  5. RELEASE_LOCK('match_apply_{matchId}')

  6. PG사 결제 API 호출
     6-1. 결제 성공 → status = 'CONFIRMED' UPDATE
     6-2. 결제 실패 → status = 'CANCELLED' UPDATE

  7. 결제 성공 시 알림 발송
	 */
	@Transactional
	public void joinMatch(final Long matchId, final Long userId) {
		FutsalMatch futsalMatch = matchRepository.findById(matchId)
			.orElseThrow(() -> new IllegalArgumentException("매치를 찾을 수 없습니다."));

		int count = (int) matchParticipantRepository.findAllByMatchId(matchId).stream()
			.filter(participant -> participant.getStatus().equals(ParticipantStatus.PENDING)
				|| participant.getStatus().equals(ParticipantStatus.CONFIRMED))
			.count();

		if (futsalMatch.isApply(count)) {
			MatchParticipant participant = MatchParticipant.builder()
				.status(ParticipantStatus.PENDING)
				.matchId(matchId)
				.userId(userId)
				.build();

			matchParticipantRepository.save(participant);
		}

		paymentClient.pay();
	}

	@Transactional
	public void joinMatchWithOptimisticLock(final Long matchId, final Long userId) {
		FutsalMatch futsalMatch = matchRepository.findById(matchId)
			.orElseThrow(() -> new IllegalArgumentException("매치를 찾을 수 없습니다."));

		if (futsalMatch.isApply(futsalMatch.getCurrentCount())) {

			matchParticipantRepository.save(MatchParticipant.builder()
				.status(ParticipantStatus.PENDING)
				.matchId(matchId)
				.userId(userId)
				.build());

			futsalMatch.increaseCount(); // currentCount 증가 → @Version 충돌 트리거
		}
	}

	@Transactional
	public void joinMatchWithNamedLock(final Long matchId, final Long userId) {
		FutsalMatch futsalMatch = matchRepository.findById(matchId)
			.orElseThrow(() -> new IllegalArgumentException("매치를 찾을 수 없습니다."));

		int count = (int) matchParticipantRepository.findAllByMatchId(matchId).stream()
			.filter(participant -> participant.getStatus().equals(ParticipantStatus.PENDING)
				|| participant.getStatus().equals(ParticipantStatus.CONFIRMED))
			.count();

		if (futsalMatch.isApply(count)) {
			matchParticipantRepository.save(MatchParticipant.builder()
				.status(ParticipantStatus.PENDING)
				.matchId(matchId)
				.userId(userId)
				.build());
		}
	}

	@Transactional
	public void joinMatchWithPessimisticLock(final Long matchId, final Long userId) {
		FutsalMatch futsalMatch = matchRepository.findByIdWithPessimisticLock(matchId)
			.orElseThrow(() -> new IllegalArgumentException("매치를 찾을 수 없습니다."));

		int count = (int) matchParticipantRepository.findAllByMatchId(matchId).stream()
			.filter(participant -> participant.getStatus().equals(ParticipantStatus.PENDING)
				|| participant.getStatus().equals(ParticipantStatus.CONFIRMED))
			.count();

		log.info("count = {}", count);

		if (futsalMatch.isApply(count)) {
			MatchParticipant participant = MatchParticipant.builder()
				.status(ParticipantStatus.PENDING)
				.matchId(matchId)
				.userId(userId)
				.build();

			matchParticipantRepository.save(participant);
		}

		// paymentClient.pay();갭
	}

	@Transactional
	public void joinMatchWithPessimisticLockAndGapLock(final Long matchId, final Long userId) {
		FutsalMatch futsalMatch = matchRepository.findById(matchId)
			.orElseThrow(() -> new IllegalArgumentException("매치를 찾을 수 없습니다."));

		int count = (int) matchParticipantRepository.findAllByMatchId(matchId).stream()
			.filter(participant -> participant.getStatus().equals(ParticipantStatus.PENDING)
				|| participant.getStatus().equals(ParticipantStatus.CONFIRMED))
			.count();

		log.info("count = {}", count);

		if (futsalMatch.isApply(count)) {
			MatchParticipant participant = MatchParticipant.builder()
				.status(ParticipantStatus.PENDING)
				.matchId(matchId)
				.userId(userId)
				.build();

			matchParticipantRepository.save(participant);
		}

		// paymentClient.pay();갭
	}
}
