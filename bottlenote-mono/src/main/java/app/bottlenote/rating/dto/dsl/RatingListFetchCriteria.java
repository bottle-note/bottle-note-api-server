package app.bottlenote.rating.dto.dsl;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.rating.constant.SearchSortType;
import app.bottlenote.rating.dto.request.RatingListFetchRequest;

/**
 * 평점 목록 리포지토리 계층 전달용 criteria.
 *
 * <p>{@code seed}: RANDOM 정렬 시 HMAC 커서 extra 또는 서버 생성값. 비-RANDOM은 0.
 */
public record RatingListFetchCriteria(
    String keyword,
    AlcoholCategoryGroup category,
    Long regionId,
    SearchSortType sortType,
    SortOrder sortOrder,
    String cursor,
    Integer size,
    Long userId,
    Long seed) {
  public static RatingListFetchCriteria of(RatingListFetchRequest request, Long userId, long seed) {
    return new RatingListFetchCriteria(
        request.keyword(),
        request.category(),
        request.regionId(),
        request.sortType(),
        request.sortOrder(),
        request.cursor(),
        request.size(),
        userId,
        seed);
  }

  /** 커서 컨텍스트 해시 - seed는 페이지마다 값이 달라 넣지 않는다. */
  public String context() {
    return "rating.list:"
        + userId
        + ":"
        + keyword
        + ":"
        + category
        + ":"
        + regionId
        + ":"
        + sortType
        + ":"
        + sortOrder;
  }
}
