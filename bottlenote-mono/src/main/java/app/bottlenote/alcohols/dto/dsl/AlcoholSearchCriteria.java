package app.bottlenote.alcohols.dto.dsl;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.alcohols.constant.SearchSortType;
import app.bottlenote.alcohols.dto.request.AlcoholSearchRequest;
import app.bottlenote.global.service.cursor.SortOrder;
import java.util.Set;

public record AlcoholSearchCriteria(
    String keyword,
    Long curationId,
    AlcoholCategoryGroup category,
    Long regionId,
    SearchSortType sortType,
    SortOrder sortOrder,
    Long cursor,
    Long pageSize,
    Long userId) {
  public static AlcoholSearchCriteria of(AlcoholSearchRequest request, Long userId) {
    return new AlcoholSearchCriteria(
        request.keyword(),
        request.curationId(),
        request.category(),
        request.regionId(),
        request.sortType(),
        request.sortOrder(),
        request.cursor(),
        request.pageSize(),
        userId);
  }
}
