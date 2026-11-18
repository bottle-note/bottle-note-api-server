package app.bottlenote.review.dto.request;

import app.bottlenote.global.pagination.KeysetPageRequest;
import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.review.constant.ReviewSortType;
import lombok.Builder;

public record ReviewPageableRequest(
    ReviewSortType sortType, SortOrder sortOrder, String cursor, Integer size) {

  public static final int DEFAULT_SIZE = 10;
  public static final int MAX_SIZE = 100;

  @Builder
  public ReviewPageableRequest {
    sortType = sortType != null ? sortType : ReviewSortType.POPULAR;
    sortOrder = sortOrder != null ? sortOrder : SortOrder.DESC;
    KeysetPageRequest page = KeysetPageRequest.of(cursor, size, DEFAULT_SIZE, MAX_SIZE);
    cursor = page.cursor();
    size = page.size();
  }
}
