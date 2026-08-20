package app.bottlenote.alcohols.dto.dsl;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.alcohols.constant.SearchSortType;
import app.bottlenote.alcohols.dto.request.ExploreStandardRequest;
import app.bottlenote.global.service.cursor.SortOrder;
import java.math.BigDecimal;
import java.util.List;

/**
 * 둘러보기 리포지토리 계층 전달용 criteria. 요청 DTO는 서비스가 {@link #of} 로 변환한다.
 *
 * <p>{@code seed}: RANDOM 정렬 시 HMAC 커서 extra 또는 서버 생성값. 비-RANDOM은 0.
 */
public record ExploreStandardCriteria(
    Long userId,
    List<String> keywords,
    AlcoholCategoryGroup category,
    List<Long> regionIds,
    List<Long> distilleryIds,
    Long curationId,
    SearchSortType sortType,
    SortOrder sortOrder,
    BigDecimal rating,
    Long seed,
    String cursor,
    Integer size) {

  public static ExploreStandardCriteria of(ExploreStandardRequest request, Long userId, long seed) {
    return new ExploreStandardCriteria(
        userId,
        request.keywords(),
        request.category(),
        request.regionIds(),
        request.distilleryIds(),
        request.curationId(),
        request.sortType(),
        request.sortOrder(),
        request.rating(),
        seed,
        request.cursor(),
        request.size());
  }

  public String context() {
    return "alcohol.explore:"
        + userId
        + ":"
        + keywords
        + ":"
        + category
        + ":"
        + regionIds
        + ":"
        + distilleryIds
        + ":"
        + curationId
        + ":"
        + sortType
        + ":"
        + sortOrder
        + ":"
        + rating;
  }
}
