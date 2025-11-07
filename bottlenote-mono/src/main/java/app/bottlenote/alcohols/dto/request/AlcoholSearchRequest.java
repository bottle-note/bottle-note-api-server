package app.bottlenote.alcohols.dto.request;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.alcohols.constant.SearchSortType;
import app.bottlenote.global.service.cursor.SortOrder;
import lombok.Builder;

public record AlcoholSearchRequest(
    String keyword,
    Long curationId,
    AlcoholCategoryGroup category,
    Long regionId,
    SearchSortType sortType,
    SortOrder sortOrder,
    Long cursor,
    Long pageSize) {
  @Builder
  public AlcoholSearchRequest {
    sortType = sortType != null ? sortType : SearchSortType.POPULAR;
    sortOrder = sortOrder != null ? sortOrder : SortOrder.DESC;
    cursor = cursor != null ? cursor : 0L;
    pageSize = pageSize != null ? pageSize : 10L;
  }
}
