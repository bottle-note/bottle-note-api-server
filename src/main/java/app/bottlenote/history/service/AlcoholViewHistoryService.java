package app.bottlenote.history.service;


import app.bottlenote.alcohols.dto.response.AlcoholDetailItem;
import app.bottlenote.global.redis.entity.AlcoholViewHistory;
import app.bottlenote.global.redis.repository.RedisAlcoholViewHistoryRepository;
import app.bottlenote.history.domain.AlcoholsViewHistory;
import app.bottlenote.history.domain.AlcoholsViewHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlcoholViewHistoryService {
	private static final String VIEW_COUNT_KEY = "stats:alcohol:views";
	private static final long DEFAULT_HISTORY_TTL = 30;
	private final RedisAlcoholViewHistoryRepository redisViewHistoryRepository;
	private final AlcoholsViewHistoryRepository historyRepository;
	private final RedisTemplate<String, Object> redisTemplate;

	/**
	 * 사용자의 주류 조회 기록 저장
	 */
	@Transactional
	public void recordView(Long userId, AlcoholDetailItem alcohol) {
		// 게스트 유저(-1)는 기록하지 않음
		if (userId <= 0) {
			return;
		}

		// 조회 기록 생성
		AlcoholViewHistory viewHistory = AlcoholViewHistory.builder()
				.id(UUID.randomUUID())
				.userId(userId)
				.alcoholId(alcohol.getAlcoholId())
				.alcoholName(alcohol.getKorName())
				.viewTime(System.currentTimeMillis())
				.ttl(DEFAULT_HISTORY_TTL)
				.build();

		// 저장
		redisViewHistoryRepository.save(viewHistory);

		// RedisTemplate 활용한 통계 기록 - 주류별 조회수 증가
		String alcoholViewKey = VIEW_COUNT_KEY + ":" + alcohol.getAlcoholId();
		redisTemplate.opsForValue().increment(alcoholViewKey);

		// 인기 주류 순위에 반영 (Sorted Set 사용)
		//String popularAlcoholsKey = "popular:alcohols:" + alcohol.getType().name().toLowerCase();
		//redisTemplate.opsForZSet().incrementScore(popularAlcoholsKey, alcohol.getId(), 1);

		log.debug("Recorded alcohol view: userId={}, alcoholId={}", userId, alcohol.getAlcoholId());
	}

	/**
	 * Redis에 저장된 조회 기록을 DB에 동기화
	 */
	@Transactional
	public void syncViewHistoryFromRedisToDb() {
		// 1. Redis에서 모든 조회 기록 가져오기
		Iterable<AlcoholViewHistory> redisHistories = redisViewHistoryRepository.findAll();
		List<AlcoholsViewHistory> entitiesToSave = new ArrayList<>();
		List<UUID> keysToDelete = new ArrayList<>();

		// 2. Redis 데이터를 DB 엔티티로 변환
		redisHistories.forEach(redisHistory -> {
			var viewTime = LocalDateTime
					.ofInstant(
							Instant.ofEpochMilli(redisHistory.getViewTime()),
							ZoneId.systemDefault()
					);
			var historyEntity = AlcoholsViewHistory.of(redisHistory.getUserId(), redisHistory.getAlcoholId(), viewTime);
			entitiesToSave.add(historyEntity);
			// 처리된 항목의 ID를 삭제 목록에 추가
			keysToDelete.add(redisHistory.getId());
		});

		// 3. DB에 저장
		if (!entitiesToSave.isEmpty()) {
			historyRepository.saveAll(entitiesToSave);
			// 4. 처리 완료된 Redis 데이터 삭제
			if (!keysToDelete.isEmpty()) {
				redisViewHistoryRepository.deleteAllById(keysToDelete);
				log.info("Redis에서 {}개 처리된 조회 기록 삭제 완료", keysToDelete.size());
			}
		}
	}
}
