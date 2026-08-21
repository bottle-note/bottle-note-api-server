package app.bottlenote.alcohols.dto.request;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.alcohols.constant.SearchSortType;
import app.bottlenote.global.pagination.KeysetPageRequest;
import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.global.validation.RatingRangeValidator;
import jakarta.validation.constraints.AssertTrue;
import java.math.BigDecimal;
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
 * <p>RANDOM 시드는 요청이 아니라 HMAC 커서 extra에 실어 다음 페이지로만 이어진다.
 */
public record ExploreStandardRequest(
    List<String> keywords,
    AlcoholCategoryGroup category,
    List<Long> regionIds,
    List<Long> distilleryIds,
    Long curationId,
    SearchSortType sortType,
    SortOrder sortOrder,
    BigDecimal ratingFrom,
    BigDecimal ratingTo,
    String cursor,
    Integer size) {

  public static final int DEFAULT_SIZE = 20;
  public static final int MAX_SIZE = 100;

  @Builder
  public ExploreStandardRequest {
    keywords = keywords != null ? List.copyOf(keywords) : List.of();
    regionIds = regionIds != null ? List.copyOf(regionIds) : List.of();
    distilleryIds = distilleryIds != null ? List.copyOf(distilleryIds) : List.of();
    sortType = sortType != null ? sortType : SearchSortType.RANDOM;
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
