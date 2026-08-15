package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.alcohols.domain.AlcoholLookupSnapshotStore;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.dto.response.AlcoholLookupItem;
import app.bottlenote.alcohols.dto.response.AlcoholLookupSnapshotItem;
import app.bottlenote.alcohols.service.AlcoholLookupService.AlcoholLookupSyncResult;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlcoholLookupSnapshotService {
  private final AlcoholQueryRepository alcoholQueryRepository;
  private final AlcoholLookupSnapshotStore snapshotStore;
  private final AtomicReference<LocalLookupSnapshot> localSnapshot =
      new AtomicReference<>(LocalLookupSnapshot.empty());

  @Value("${alcohol.lookup.local-cache.enabled:false}")
  private boolean localCacheEnabled;

  @Value("${alcohol.lookup.local-cache.version-check-interval-ms:1000}")
  private long localCacheVersionCheckIntervalMs;

  @Transactional(readOnly = true)
  public List<AlcoholLookupItem> findFilteredItems(
      String keyword, AlcoholCategoryGroup categoryGroup, Long regionId, Long distilleryId) {
    List<String> keywords = parseKeywords(keyword);
    return findItems().stream()
        .filter(item -> matchesKeywords(item, keywords))
        .filter(item -> categoryGroup == null || categoryGroup == item.categoryGroup())
        .filter(item -> regionId == null || regionId.equals(item.regionId()))
        .filter(item -> distilleryId == null || distilleryId.equals(item.distilleryId()))
        .sorted(java.util.Comparator.comparing(AlcoholLookupSnapshotItem::alcoholId))
        .map(AlcoholLookupSnapshotItem::toLookupItem)
        .toList();
  }

  @Transactional(readOnly = true)
  public AlcoholLookupSyncResult syncSnapshot() {
    List<AlcoholLookupSnapshotItem> items = findDatabaseItems();
    if (items.isEmpty()) {
      log.warn("Alcohol lookup DB 원천 데이터가 0건이라 Redis snapshot 갱신을 건너뜁니다.");
      return AlcoholLookupSyncResult.unchanged(0);
    }

    try {
      List<AlcoholLookupSnapshotItem> currentItems = snapshotStore.findAll();
      if (currentItems.equals(items)) {
        return AlcoholLookupSyncResult.unchanged(items.size());
      }
    } catch (Exception e) {
      log.warn("Alcohol lookup 기존 snapshot 비교 실패. 새 snapshot으로 갱신합니다.", e);
    }

    snapshotStore.replaceAll(items);
    return AlcoholLookupSyncResult.changed(items.size());
  }

  private List<AlcoholLookupSnapshotItem> findItems() {
    if (localCacheEnabled) {
      return findLocalCachedItemsWithFallback();
    }
    return findRedisItemsWithFallback();
  }

  private List<AlcoholLookupSnapshotItem> findLocalCachedItemsWithFallback() {
    long now = System.currentTimeMillis();
    LocalLookupSnapshot cached = localSnapshot.get();
    if (cached.isFresh(now, localCacheVersionCheckIntervalMs)) {
      return cached.items();
    }

    try {
      Optional<String> redisVersion = snapshotStore.findVersion();
      if (cached.hasItems()
          && redisVersion.isPresent()
          && redisVersion.get().equals(cached.version())) {
        localSnapshot.set(cached.checkedAt(now));
        return cached.items();
      }

      List<AlcoholLookupSnapshotItem> items = snapshotStore.findAll();
      if (!items.isEmpty()) {
        localSnapshot.set(LocalLookupSnapshot.of(redisVersion.orElse("unversioned"), items, now));
        return items;
      }
      log.info("Alcohol lookup Redis snapshot이 비어 있어 fallback 경로를 사용합니다.");
    } catch (Exception e) {
      log.warn("Alcohol lookup local cache 갱신 실패. fallback 경로를 사용합니다.", e);
    }
    if (cached.hasItems()) {
      localSnapshot.set(cached.checkedAt(now));
      log.warn("Alcohol lookup stale local cache를 사용합니다. Redis/DB 부하 보호를 우선합니다.");
      return cached.items();
    }
    return findDatabaseItems();
  }

  private List<AlcoholLookupSnapshotItem> findRedisItemsWithFallback() {
    try {
      List<AlcoholLookupSnapshotItem> items = snapshotStore.findAll();
      if (!items.isEmpty()) {
        return items;
      }
      log.info("Alcohol lookup Redis snapshot이 비어 있어 DB fallback 경로를 사용합니다.");
    } catch (Exception e) {
      log.warn("Alcohol lookup Redis snapshot 조회 실패. DB fallback 경로를 사용합니다.", e);
    }
    return findDatabaseItems();
  }

  private List<AlcoholLookupSnapshotItem> findDatabaseItems() {
    return alcoholQueryRepository.findAllLookupItems().stream()
        .map(AlcoholLookupSnapshotItem::from)
        .toList();
  }

  private List<String> parseKeywords(String keyword) {
    if (keyword == null || keyword.isBlank()) {
      return List.of();
    }
    return Arrays.stream(keyword.trim().toLowerCase(Locale.ROOT).split("\\s+"))
        .filter(value -> !value.isBlank())
        .toList();
  }

  private boolean matchesKeywords(AlcoholLookupSnapshotItem item, List<String> keywords) {
    if (keywords.isEmpty()) {
      return true;
    }
    return keywords.stream().allMatch(item.normalizedSearchText()::contains);
  }

  private record LocalLookupSnapshot(
      String version, List<AlcoholLookupSnapshotItem> items, long checkedAtMillis) {

    private static LocalLookupSnapshot empty() {
      return new LocalLookupSnapshot("", List.of(), 0L);
    }

    private static LocalLookupSnapshot of(
        String version, List<AlcoholLookupSnapshotItem> items, long checkedAtMillis) {
      return new LocalLookupSnapshot(version, List.copyOf(items), checkedAtMillis);
    }

    private boolean hasItems() {
      return !items.isEmpty();
    }

    private boolean isFresh(long now, long checkIntervalMillis) {
      return hasItems() && now - checkedAtMillis < Math.max(checkIntervalMillis, 0L);
    }

    private LocalLookupSnapshot checkedAt(long checkedAtMillis) {
      return new LocalLookupSnapshot(version, items, checkedAtMillis);
    }
  }
}
