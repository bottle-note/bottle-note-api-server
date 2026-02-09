package app.bottlenote.history.service;

import app.bottlenote.alcohols.dto.response.AlcoholDetailItem;
import app.bottlenote.alcohols.dto.response.ViewHistoryItem;
import app.bottlenote.global.data.response.CollectionResponse;
import app.bottlenote.global.redis.entity.AlcoholViewHistory;
import app.bottlenote.global.redis.repository.RedisAlcoholViewHistoryRepository;
import app.bottlenote.history.domain.AlcoholsViewHistory;
import app.bottlenote.history.domain.AlcoholsViewHistory.AlcoholsViewHistoryId;
import app.bottlenote.history.domain.AlcoholsViewHistoryRepository;
import app.bottlenote.observability.annotation.SkipTrace;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlcoholViewHistoryService {
  private static final String VIEW_COUNT_KEY = "stats:alcohol:views";
  private static final long DEFAULT_HISTORY_TTL = 30;
  private final RedisAlcoholViewHistoryRepository redisViewHistoryRepository;
  private final AlcoholsViewHistoryRepository historyRepository;
  private final RedisTemplate<String, Object> redisTemplate;
  private final EntityManager entityManager;

  /** 사용자의 주류 조회 기록 저장 */
  @Transactional
  public void recordView(Long userId, AlcoholDetailItem alcohol) {
    // 게스트 유저(-1)는 기록하지 않음
    if (userId <= 0) {
      return;
    }

    // 조회 기록 생성
    AlcoholViewHistory viewHistory =
        AlcoholViewHistory.builder()
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
    // String popularAlcoholsKey = "popular:alcohols:" + alcohol.getType().name().toLowerCase();
    // redisTemplate.opsForZSet().incrementScore(popularAlcoholsKey, alcohol.getId(), 1);

    log.debug("Recorded alcohol view: userId={}, alcoholId={}", userId, alcohol.getAlcoholId());
  }

  /** Redis에 저장된 조회 기록을 DB에 동기화 */
  @SkipTrace
  @Transactional
  public void syncViewHistoryFromRedisToDb() {
    Iterable<AlcoholViewHistory> redisHistories = redisViewHistoryRepository.findAll();

    // 동일 (userId, alcoholId) 쌍에서 최신 viewTime만 유지
    Map<AlcoholsViewHistoryId, RedisEntry> latestEntries = new HashMap<>();

    redisHistories.forEach(
        redisHistory -> {
          if (redisHistory == null) {
            log.debug("TTL expired data found in index, skipping...");
            return;
          }

          var viewTime =
              LocalDateTime.ofInstant(
                  Instant.ofEpochMilli(redisHistory.getViewTime()), ZoneId.systemDefault());
          var compositeKey =
              AlcoholsViewHistoryId.of(redisHistory.getUserId(), redisHistory.getAlcoholId());

          latestEntries.merge(
              compositeKey,
              new RedisEntry(viewTime, redisHistory.getId()),
              (existing, incoming) ->
                  incoming.viewTime.isAfter(existing.viewTime) ? incoming : existing);
        });

    List<UUID> successKeys = new ArrayList<>();

    for (var entry : latestEntries.entrySet()) {
      var compositeKey = entry.getKey();
      var redisEntry = entry.getValue();

      try {
        historyRepository
            .findById(compositeKey)
            .ifPresentOrElse(
                existing -> existing.updateViewAt(redisEntry.viewTime),
                () ->
                    historyRepository.save(
                        AlcoholsViewHistory.of(
                            compositeKey.getUserId(),
                            compositeKey.getAlcoholId(),
                            redisEntry.viewTime)));
        // flush로 즉시 SQL 실행하여 에러를 개별 catch로 격리
        entityManager.flush();
        successKeys.add(redisEntry.redisId);
      } catch (Exception e) {
        // flush 시점에 발생한 에러를 격리하고 해당 엔트리의 영속성 컨텍스트를 초기화
        entityManager.clear();
        log.error(
            "조회 기록 동기화 실패: userId={}, alcoholId={}, error={}",
            compositeKey.getUserId(),
            compositeKey.getAlcoholId(),
            e.getMessage());
      }
    }

    // DB 커밋 성공 후에만 Redis에서 삭제
    if (!successKeys.isEmpty()) {
      List<UUID> keysToDelete = List.copyOf(successKeys);
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              redisViewHistoryRepository.deleteAllById(keysToDelete);
              log.info("Redis에서 {}개 처리된 조회 기록 삭제 완료", keysToDelete.size());
            }
          });
    }
  }

  private record RedisEntry(LocalDateTime viewTime, UUID redisId) {}

  @Transactional(readOnly = true)
  public CollectionResponse<ViewHistoryItem> getViewHistory(Long id) {
    Pageable pageable = Pageable.ofSize(6);
    var totalCount = historyRepository.countByUserId(id);
    var allByUserId = historyRepository.findAllByUserId(id, pageable);
    return CollectionResponse.of(totalCount, allByUserId);
  }
}
