package app.bottlenote.alcohols.dto.request;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.alcohols.constant.SearchSortType;
import app.bottlenote.global.service.cursor.SortOrder;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import lombok.Builder;

/**
 * 위스키 둘러보기 요청.
 *
 * <p>필터 결합 규칙:
 *
 * <ul>
 *   <li>{@code keywords}: 다중 키워드 간 <b>AND</b> (각 키워드는 여러 필드와 OR 매칭)
 *   <li>{@code regionIds}, {@code distilleryIds}: 컬렉션 내 값 간 <b>OR</b> (IN 절)
 *   <li>서로 다른 필터 간: <b>AND</b>
 * </ul>
 *
 * <p>{@code seed}: {@link SearchSortType#RANDOM} 정렬 시 페이지 간 순서 일관성을 위한 값. 미전송 시 서버가 생성하여 응답 meta 에
 * 실어 내려준다. 클라이언트는 첫 응답의 seed 를 이후 페이지 요청에 그대로 전달하여 동일한 순서를 재현한다. 비-RANDOM 정렬에서는 무시된다.
 */
public record ExploreStandardRequest(
    List<String> keywords,
    AlcoholCategoryGroup category,
    List<Long> regionIds,
    List<Long> distilleryIds,
    Long curationId,
    SearchSortType sortType,
    SortOrder sortOrder,
    Long seed,
    @PositiveOrZero Long cursor,
    @Min(1) @Max(100) Integer size) {

  @Builder
  public ExploreStandardRequest {
    keywords = keywords != null ? keywords : List.of();
    regionIds = regionIds != null ? regionIds : List.of();
    distilleryIds = distilleryIds != null ? distilleryIds : List.of();
    sortType = sortType != null ? sortType : SearchSortType.RANDOM;
    sortOrder = sortOrder != null ? sortOrder : SortOrder.DESC;
    cursor = cursor != null ? cursor : 0L;
    size = size != null ? size : 20;
  }
}
