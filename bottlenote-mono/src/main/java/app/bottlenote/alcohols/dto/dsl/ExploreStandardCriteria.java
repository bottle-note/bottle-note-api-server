package app.bottlenote.alcohols.dto.dsl;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.alcohols.constant.SearchSortType;
import app.bottlenote.alcohols.dto.request.ExploreStandardRequest;
import app.bottlenote.global.service.cursor.SortOrder;
import java.util.List;

/**
 * 둘러보기 리포지토리 계층 전달용 criteria. 요청 DTO는 서비스가 {@link #of} 로 변환한다.
 *
 * <p>{@code seed}: RANDOM 정렬 시 사용할 난수 시드. Service 계층에서 null 이 아닌 값으로 확정되어 리포지토리까지 전달된다.
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
    Long seed,
    Long cursor,
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
        seed,
        request.cursor(),
        request.size());
  }

}
