package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.alcohols.domain.AlcoholLookupSnapshotStore;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.dto.request.AlcoholLookupRequest;
import app.bottlenote.alcohols.dto.response.AlcoholLookupItem;
import app.bottlenote.alcohols.dto.response.AlcoholLookupSnapshotItem;
import app.bottlenote.global.service.cursor.CursorResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlcoholLookupService {
  private final AlcoholQueryRepository alcoholQueryRepository;
  private final AlcoholLookupSnapshotStore snapshotStore;

  @Transactional(readOnly = true)
  public CursorResponse<AlcoholLookupItem> lookup(AlcoholLookupRequest request) {
    return filterAndSlice(findRedisItemsWithFallback(), request);
  }

  @Transactional(readOnly = true)
  public int syncSnapshot() {
    List<AlcoholLookupSnapshotItem> items = findDatabaseItems();
    snapshotStore.replaceAll(items);
    return items.size();
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

  private CursorResponse<AlcoholLookupItem> filterAndSlice(
      List<AlcoholLookupSnapshotItem> items, AlcoholLookupRequest request) {
    AlcoholCategoryGroup categoryGroup = request.categoryGroup();
    List<String> keywords = parseKeywords(request.keyword());
    long cursor = Math.max(request.cursor(), 0L);
    long pageSize = Math.max(request.pageSize(), 1L);

    // 성능 검증용 구조: 정규화 필드는 snapshot에 저장하되, 후속 작업에서 DTO 패키지 경계를 정리한다.
    List<AlcoholLookupItem> page =
        items.stream()
            .filter(item -> matchesKeywords(item, keywords))
            .filter(item -> categoryGroup == null || categoryGroup == item.categoryGroup())
            .filter(
                item -> request.regionId() == null || request.regionId().equals(item.regionId()))
            .filter(
                item ->
                    request.distilleryId() == null
                        || request.distilleryId().equals(item.distilleryId()))
            .skip(cursor)
            .limit(pageSize + 1)
            .map(AlcoholLookupSnapshotItem::toLookupItem)
            .toList();

    return CursorResponse.of(page, cursor, Math.toIntExact(pageSize));
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
}
