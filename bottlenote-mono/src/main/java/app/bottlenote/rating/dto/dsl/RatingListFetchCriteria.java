package app.bottlenote.rating.dto.dsl;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.rating.constant.SearchSortType;
import app.bottlenote.rating.dto.request.RatingListFetchRequest;

public record RatingListFetchCriteria(
    String keyword,
    AlcoholCategoryGroup category,
    Long regionId,
    SearchSortType sortType,
    SortOrder sortOrder,
    String cursor,
    Integer size,
    Long userId) {
  public static RatingListFetchCriteria of(RatingListFetchRequest request, Long userId) {
    return new RatingListFetchCriteria(
        request.keyword(),
        request.category(),
        request.regionId(),
        request.sortType(),
        request.sortOrder(),
        request.cursor(),
        request.size(),
        userId);
  }
}
