package app.bottlenote.rating.dto.request;

import app.bottlenote.rating.constant.SearchSortType;
import app.bottlenote.shared.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.shared.cursor.SortOrder;
import lombok.Builder;

public record RatingListFetchRequest(
    String keyword,
    AlcoholCategoryGroup category,
    Long regionId,
    SearchSortType sortType,
    SortOrder sortOrder,
    Long cursor,
    Long pageSize) {

  @Builder
  public RatingListFetchRequest {
    sortType = sortType != null ? sortType : SearchSortType.RANDOM;
    sortOrder = sortOrder != null ? sortOrder : SortOrder.DESC;
    cursor = cursor != null ? cursor : 0L;
    pageSize = pageSize != null ? pageSize : 10L;
  }
}
