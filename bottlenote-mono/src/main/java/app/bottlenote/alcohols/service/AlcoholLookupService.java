package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.constant.AlcoholLookupSource.DATABASE;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.alcohols.domain.AlcoholLookupSnapshotStore;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.dto.request.AlcoholLookupRequest;
import app.bottlenote.alcohols.dto.response.AlcoholLookupItem;
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
    List<AlcoholLookupItem> sourceItems =
        request.source() == DATABASE ? findDatabaseItems() : findRedisItemsWithFallback();
    return filterAndSlice(sourceItems, request);
  }

  @Transactional(readOnly = true)
  public CursorResponse<AlcoholLookupItem> lookupFromDatabase(AlcoholLookupRequest request) {
    return filterAndSlice(findDatabaseItems(), request);
  }

  @Transactional(readOnly = true)
  public int syncSnapshot() {
    List<AlcoholLookupItem> items = findDatabaseItems();
    snapshotStore.replaceAll(items);
    return items.size();
  }

  private List<AlcoholLookupItem> findRedisItemsWithFallback() {
    try {
      List<AlcoholLookupItem> items = snapshotStore.findAll();
      if (!items.isEmpty()) {
        return items;
      }
      log.info("Alcohol lookup Redis snapshot이 비어 있어 DB fallback 경로를 사용합니다.");
    } catch (Exception e) {
      log.warn("Alcohol lookup Redis snapshot 조회 실패. DB fallback 경로를 사용합니다.", e);
    }
    return findDatabaseItems();
  }

  private List<AlcoholLookupItem> findDatabaseItems() {
    return alcoholQueryRepository.findAllLookupItems();
  }

  private CursorResponse<AlcoholLookupItem> filterAndSlice(
      List<AlcoholLookupItem> items, AlcoholLookupRequest request) {
    AlcoholCategoryGroup categoryGroup = request.categoryGroup();
    List<String> keywords = parseKeywords(request.keyword());
    long cursor = Math.max(request.cursor(), 0L);
    long pageSize = Math.max(request.pageSize(), 1L);

    // k6 비교 기준: Redis와 DB 경로 모두 동일한 JVM stream 필터링 비용을 사용하고 I/O source만 분리한다.
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

  private boolean matchesKeywords(AlcoholLookupItem item, List<String> keywords) {
    if (keywords.isEmpty()) {
      return true;
    }
    String searchText = item.searchText();
    return keywords.stream().allMatch(searchText::contains);
  }
}
