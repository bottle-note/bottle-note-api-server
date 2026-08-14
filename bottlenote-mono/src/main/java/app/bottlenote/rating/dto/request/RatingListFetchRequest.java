package app.bottlenote.rating.dto.request;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.global.pagination.PaginationRequest;
import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.rating.constant.SearchSortType;
import lombok.Builder;

public record RatingListFetchRequest(
    String keyword,
    AlcoholCategoryGroup category,
    Long regionId,
    SearchSortType sortType,
    SortOrder sortOrder,
    String cursor,
    Integer size) {

  public static final int DEFAULT_SIZE = 10;
  public static final int MAX_SIZE = 100;

  @Builder
  public RatingListFetchRequest {
    sortType = sortType != null ? sortType : SearchSortType.RANDOM;
    sortOrder = sortOrder != null ? sortOrder : SortOrder.DESC;
    PaginationRequest page = PaginationRequest.of(cursor, size, DEFAULT_SIZE, MAX_SIZE);
    cursor = page.cursor();
    size = page.size();
  }
}
