package app.bottlenote.review.dto.dsl;

import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.review.constant.ReviewSortType;
import app.bottlenote.review.dto.request.ReviewExploreRequest;
import java.math.BigDecimal;
import java.util.List;

/** 리뷰 둘러보기의 request-to-repository 전달 계약. */
public record ReviewExploreCriteria(
    Long userId,
    String keyword,
    List<String> keywords,
    ReviewSortType sortType,
    SortOrder sortOrder,
    BigDecimal rating,
    String cursor,
    Integer size) {

  public static ReviewExploreCriteria of(ReviewExploreRequest request, Long userId) {
    return new ReviewExploreCriteria(
        userId,
        request.keyword(),
        request.keywords(),
        request.sortType(),
        request.sortOrder(),
        request.rating(),
        request.cursor(),
        request.size());
  }

  public List<String> effectiveKeywords() {
    return keyword == null ? keywords : List.of(keyword);
  }

  public String context() {
    return "review.explore:"
        + userId
        + ":"
        + effectiveKeywords()
        + ":"
        + sortType
        + ":"
        + sortOrder
        + ":"
        + rating;
  }
}
