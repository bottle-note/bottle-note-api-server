package app.bottlenote.review.dto.request;

import app.bottlenote.global.pagination.KeysetPageRequest;
import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.global.validation.RatingRangeValidator;
import app.bottlenote.review.constant.ReviewSortType;
import jakarta.validation.constraints.AssertTrue;
import java.math.BigDecimal;
import lombok.Builder;

public record ReviewExploreRequest(
    String keyword,
    ReviewSortType sortType,
    SortOrder sortOrder,
    BigDecimal ratingFrom,
    BigDecimal ratingTo,
    String cursor,
    Integer size) {

  public static final int DEFAULT_SIZE = 20;
  public static final int MAX_SIZE = 100;

  @Builder
  public ReviewExploreRequest {
    keyword = keyword != null && !keyword.isBlank() ? keyword.trim() : null;
    sortType = sortType != null ? sortType : ReviewSortType.LATEST;
    sortOrder = sortOrder != null ? sortOrder : SortOrder.DESC;
    KeysetPageRequest page = KeysetPageRequest.of(cursor, size, DEFAULT_SIZE, MAX_SIZE);
    cursor = page.cursor();
    size = page.size();
  }

  @AssertTrue(message = "EXPLORE_RATING_INVALID")
  public boolean hasValidRatingRange() {
    return RatingRangeValidator.isValid(ratingFrom, ratingTo);
  }
}
