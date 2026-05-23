package app.bottlenote.alcohols.service;

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
    return filterAndSlice(findRedisItemsWithFallback(), request);
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
