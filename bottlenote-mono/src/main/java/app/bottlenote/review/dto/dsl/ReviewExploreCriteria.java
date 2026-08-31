package app.bottlenote.review.dto.dsl;

import app.bottlenote.global.search.SearchKeywordTokenizer;
import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.review.constant.ReviewSortType;
import app.bottlenote.review.dto.request.ReviewExploreRequest;
import java.math.BigDecimal;
import java.util.List;

/** 리뷰 둘러보기의 request-to-repository 전달 계약. */
public record ReviewExploreCriteria(
    Long userId,
    List<String> searchTokens,
    ReviewSortType sortType,
    SortOrder sortOrder,
    BigDecimal ratingFrom,
    BigDecimal ratingTo,
    String cursor,
    Integer size) {

  public static ReviewExploreCriteria of(ReviewExploreRequest request, Long userId) {
    return new ReviewExploreCriteria(
        userId,
        SearchKeywordTokenizer.tokenize(request.keyword()),
        request.sortType(),
        request.sortOrder(),
        request.ratingFrom(),
        request.ratingTo(),
        request.cursor(),
        request.size());
  }

  public String context() {
    return "review.explore:"
        + userId
        + ":"
        + searchTokens
        + ":"
        + sortType
        + ":"
        + sortOrder
        + ":"
        + ratingFrom
        + ":"
        + ratingTo;
  }
}
