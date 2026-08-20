package app.bottlenote.review.dto.request;

import app.bottlenote.global.pagination.PaginationRequest;
import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.review.constant.ReviewSortType;
import jakarta.validation.constraints.AssertTrue;
import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;

public record ReviewExploreRequest(
    String keyword,
    List<String> keywords,
    ReviewSortType sortType,
    SortOrder sortOrder,
    BigDecimal rating,
    String cursor,
    Integer size) {

  public static final int DEFAULT_SIZE = 20;
  public static final int MAX_SIZE = 100;

  @Builder
  public ReviewExploreRequest {
    keyword = keyword != null && !keyword.isBlank() ? keyword.trim() : null;
    keywords = keywords != null ? List.copyOf(keywords) : List.of();
    sortType = sortType != null ? sortType : ReviewSortType.POPULAR;
    sortOrder = sortOrder != null ? sortOrder : SortOrder.DESC;
    PaginationRequest page = PaginationRequest.of(cursor, size, DEFAULT_SIZE, MAX_SIZE);
    cursor = page.cursor();
    size = page.size();
  }

  @AssertTrue(message = "EXPLORE_KEYWORD_CONFLICT")
  public boolean hasNoKeywordConflict() {
    return keyword == null || keywords.isEmpty();
  }

  @AssertTrue(message = "EXPLORE_RATING_INVALID")
  public boolean hasValidRating() {
    return rating == null
        || (rating.compareTo(new BigDecimal("0.5")) >= 0
            && rating.compareTo(new BigDecimal("5.0")) <= 0
            && rating.remainder(new BigDecimal("0.5")).compareTo(BigDecimal.ZERO) == 0);
  }
}
