package app.bottlenote.rating.dto.dsl;

import app.bottlenote.rating.dto.request.RatingListFetchRequest;
import app.bottlenote.shared.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.shared.cursor.SortOrder;
import app.bottlenote.shared.rating.constant.SearchSortType;

public record RatingListFetchCriteria(
    String keyword,
    AlcoholCategoryGroup category,
    Long regionId,
    SearchSortType sortType,
    SortOrder sortOrder,
    Long cursor,
    Long pageSize,
    Long userId) {
  public static RatingListFetchCriteria of(RatingListFetchRequest request, Long userId) {
    return new RatingListFetchCriteria(
        request.keyword(),
        request.category(),
        request.regionId(),
        request.sortType(),
        request.sortOrder(),
        request.cursor(),
        request.pageSize(),
        userId);
  }
}
