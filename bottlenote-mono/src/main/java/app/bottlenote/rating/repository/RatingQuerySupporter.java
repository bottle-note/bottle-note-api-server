package app.bottlenote.rating.repository;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.picks.domain.QPicks.picks;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.domain.QReview.review;
import static com.querydsl.jpa.JPAExpressions.select;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.rating.constant.SearchSortType;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.util.StringUtils;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RatingQuerySupporter {

  private final app.bottlenote.alcohols.domain.RegionRepository regionRepository;

  /**
   * 마이 페이지 사용자의 평점 개수를 조회한다.
   *
   * @param userId 마이 페이지 사용자
   * @return 평점 개수
   */
  public Expression<Long> ratingCountSubQuery(Long userId) {
    return ExpressionUtils.as(
        select(rating.count())
            .from(rating)
            .where(rating.id.userId.eq(userId).and(rating.ratingPoint.rating.gt(0.0))),
        "ratingCount");
  }

  public Expression<Long> ratingCountSubQuery(NumberPath<Long> userId) {
    return ExpressionUtils.as(
        select(rating.count())
            .from(rating)
            .where(rating.id.userId.eq(userId).and(rating.ratingPoint.rating.gt(0.0))),
        "ratingCount");
  }

  public Expression<Double> averageRatingSubQuery(NumberPath<Long> alocholId) {
    return ExpressionUtils.as(
        select(
                rating
                    .ratingPoint
                    .rating
                    .avg()
                    .multiply(10)
                    .castToNum(Double.class)
                    .round()
                    .divide(10)
                    .coalesce(0.0))
            .from(rating)
            .where(rating.id.alcoholId.eq(alocholId).and(rating.ratingPoint.rating.gt(0.0))),
        "averageRatingPoint");
  }

  public Expression<Long> averageRatingCountSubQuery(NumberPath<Long> alocholId) {
    return ExpressionUtils.as(
        select(rating.ratingPoint.rating.count())
            .from(rating)
            .where(rating.id.alcoholId.eq(alocholId).and(rating.ratingPoint.rating.gt(0.0))),
        "averageRatingCount");
  }

  /** 술 이름을 검색하는 조건 */
  protected BooleanExpression eqAlcoholName(String name) {

    if (StringUtils.isNullOrEmpty(name)) return null;

    return alcohol.korName.like("%" + name + "%").or(alcohol.engName.like("%" + name + "%"));
  }

  /** 카테고리를 검색하는 조건 */
  protected BooleanExpression eqAlcoholCategory(AlcoholCategoryGroup category) {

    if (Objects.isNull(category)) return null;

    return alcohol.categoryGroup.stringValue().like("%" + category + "%");
  }

  /** 리전을 검색하는 조건 (부모 지역이면 하위 지역 포함) */
  protected BooleanExpression eqAlcoholRegion(Long regionId) {
    if (regionId == null) return null;
    List<Long> childIds = regionRepository.findChildRegionIds(regionId);
    if (childIds.isEmpty()) return alcohol.region.id.eq(regionId);
    List<Long> regionIds = new java.util.ArrayList<>(childIds.size() + 1);
    regionIds.add(regionId);
    regionIds.addAll(childIds);
    return alcohol.region.id.in(regionIds);
  }

  /**
   * 내가 별점을 안준 술만 조회
   *
   * @param userId the user id
   * @return the boolean expression
   */
  public BooleanExpression neRatingByMe(Long userId) {
    if (userId == null) return null;

    return rating.id.userId.isNull().or(rating.id.userId.ne(userId));
  }

  /** 1차 정렬 조건 - RANDOM - POPULAR - RATING - PICK - REVIEW */
  protected OrderSpecifier<?> orderBy(SearchSortType searchSortType, SortOrder sortOrder) {
    // 평점 없는 알코올의 AVG(rating) NULL → coalesce(0.0)로 sortScore와 표현식을 맞춰 keyset seek 정합성 보장
    NumberExpression<Double> avgRating = rating.ratingPoint.rating.avg().coalesce(0.0);
    NumberExpression<Long> reviewCount = review.id.countDistinct(); // 고유 리뷰 수 계산
    NumberExpression<Long> pickCount = picks.id.countDistinct(); // 고유 좋아요 수 계산

    return switch (searchSortType) {
      case POPULAR ->
          sortOrder == SortOrder.DESC
              ? avgRating.add(reviewCount).desc()
              : avgRating.add(reviewCount).asc();
      case RATING -> sortOrder == SortOrder.DESC ? avgRating.desc() : avgRating.asc();
      case PICK -> sortOrder == SortOrder.DESC ? pickCount.desc() : pickCount.asc();
      case REVIEW -> sortOrder == SortOrder.DESC ? reviewCount.desc() : reviewCount.asc();
      case RANDOM -> Expressions.numberTemplate(Double.class, "function('rand')").asc();
    };
  }

  /** keyset seek에 쓰는 정렬 점수. orderBy의 각 case와 동일한 표현식이어야 한다. RANDOM은 crc32Rank를 쓴다. */
  protected NumberExpression<? extends Number> sortScore(SearchSortType searchSortType) {
    NumberExpression<Double> avgRating = rating.ratingPoint.rating.avg().coalesce(0.0);
    NumberExpression<Long> reviewCount = review.id.countDistinct();
    NumberExpression<Long> pickCount = picks.id.countDistinct();
    return switch (searchSortType) {
      case POPULAR -> avgRating.add(reviewCount);
      case RATING -> avgRating;
      case PICK -> pickCount;
      case REVIEW -> reviewCount;
      case RANDOM -> throw new IllegalArgumentException("RANDOM uses crc32Rank");
    };
  }

  /** HMAC 커서 RANDOM용 CRC32 랭크. CRC32(CONCAT(seed, '-', id)) */
  protected NumberExpression<Long> crc32Rank(long seed) {
    return Expressions.numberTemplate(
        Long.class,
        "cast(function('crc32', concat({0}, '-', {1})) as long)",
        String.valueOf(seed),
        alcohol.id);
  }

  /** 2차 정렬 조건 (랜덤) */
  protected OrderSpecifier<?> orderByRandom() {
    return Expressions.numberTemplate(Double.class, "function('rand')").asc();
  }
}
