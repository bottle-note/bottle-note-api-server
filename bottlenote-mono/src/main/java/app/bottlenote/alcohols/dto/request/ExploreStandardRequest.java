package app.bottlenote.alcohols.dto.request;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.alcohols.constant.SearchSortType;
import app.bottlenote.global.service.cursor.SortOrder;
import java.util.List;
import lombok.Builder;

/**
 * 위스키 둘러보기 요청.
 *
 * <p>필터 결합 규칙:
 *
 * <ul>
 *   <li>{@code keywords}: 다중 키워드 간 <b>AND</b> (각 키워드는 여러 필드와 OR 매칭)
 *   <li>{@code regionIds}, {@code distilleryIds}: 컬렉션 내 값 간 <b>OR</b> (IN 절)
 *   <li>서로 다른 필터 간: <b>AND</b>
 * </ul>
 */
public record ExploreStandardRequest(
    List<String> keywords,
    AlcoholCategoryGroup category,
    List<Long> regionIds,
    List<Long> distilleryIds,
    Long curationId,
    SearchSortType sortType,
    SortOrder sortOrder,
    Long cursor,
    Integer size) {

  @Builder
  public ExploreStandardRequest {
    keywords = keywords != null ? keywords : List.of();
    regionIds = regionIds != null ? regionIds : List.of();
    distilleryIds = distilleryIds != null ? distilleryIds : List.of();
    sortType = sortType != null ? sortType : SearchSortType.RANDOM;
    sortOrder = sortOrder != null ? sortOrder : SortOrder.DESC;
    cursor = cursor != null ? cursor : 0L;
    size = size != null ? size : 20;
  }
}
