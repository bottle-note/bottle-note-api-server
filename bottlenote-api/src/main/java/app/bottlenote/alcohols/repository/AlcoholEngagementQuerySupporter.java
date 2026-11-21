package app.bottlenote.alcohols.repository;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.picks.constant.PicksStatus.PICK;
import static app.bottlenote.picks.domain.QPicks.picks;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.domain.QReview.review;

import app.bottlenote.alcohols.constant.SearchSortType;
import app.bottlenote.global.rating.RatingDisplay;
import app.bottlenote.global.service.cursor.SortOrder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.JPAExpressions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 주류 화면 조회에 쓰는 찜·별점·리뷰 식 모음. 여러 업무의 Q타입을 쓰므로 최종 소유는 product-api다. */
@Component
@RequiredArgsConstructor
public class AlcoholEngagementQuerySupporter {

  private final AlcoholQuerySupporter alcoholQuerySupporter;

  /** 사용자가 주류에 찜하기를 했는지 확인하는 서브쿼리 생성 */
  public BooleanExpression pickedSubQuery(Long userId) {
    if (userId == -1) return Expressions.asBoolean(false);

    return JPAExpressions.selectOne()
        .from(picks)
        .where(picks.alcoholId.eq(alcohol.id), picks.userId.eq(userId), picks.status.eq(PICK))
        .exists();
  }

  /** 특정 주류가 사용자에 의해 찜되었는지 확인 */
  public BooleanExpression isPickedSubquery(Long alcoholId, Long userId) {
    if (userId == -1) {
      return Expressions.asBoolean(false);
    }
    return Expressions.asBoolean(
            JPAExpressions.selectOne()
                .from(picks)
                .where(
                    picks
                        .alcoholId
                        .eq(alcoholId)
                        .and(picks.userId.eq(userId))
                        .and(picks.status.eq(PICK)))
                .exists())
        .as("isPicked");
  }

  /** 사용자가 주류에 준 평점 조회 */
  public NumberExpression<Double> myRating(Long alcoholId, Long userId) {
    return Expressions.asNumber(
            JPAExpressions.select(rating.ratingPoint.rating)
                .from(rating)
                .where(rating.id.alcoholId.eq(alcoholId).and(rating.id.userId.eq(userId)))
                .limit(1))
        .coalesce(0.0)
        .castToNum(Double.class)
        .as("myRating");
  }

  /** 사용자가 주류에 작성한 리뷰들의 평균 평점 계산 */
  public NumberExpression<Double> averageReviewRating(Long alcoholId, Long userId) {
    return Expressions.asNumber(
            JPAExpressions.select(RatingDisplay.normalize(review.reviewRating.avg()).coalesce(0.0))
                .from(review)
                .where(
                    review
                        .alcoholId
                        .eq(alcoholId)
                        .and(review.userId.eq(userId))
                        .and(review.reviewRating.gt(0.0))))
        .castToNum(Double.class)
        .as("myAvgRating");
  }

  /** 정렬 조건에 따른 OrderSpecifier 생성 */
  public OrderSpecifier<?> sortBy(SearchSortType searchSortType, SortOrder sortOrder) {
    // 평점 없는 알코올의 AVG(rating) NULL → ORDER BY 시 NULL-first 로 정렬 불안정 유발
    // coalesce(0.0) 로 결정적 정렬
    NumberExpression<Double> avgRating = rating.ratingPoint.rating.avg().coalesce(0.0);
    NumberExpression<Long> reviewCount = review.id.countDistinct();
    NumberExpression<Long> pickCount = picks.id.countDistinct();
    return switch (searchSortType) {
      case POPULAR -> throw new IllegalArgumentException("POPULAR uses snapshot score");
      case RATING -> sortOrder == SortOrder.DESC ? avgRating.desc() : avgRating.asc();
      case PICK -> sortOrder == SortOrder.DESC ? pickCount.desc() : pickCount.asc();
      case REVIEW -> sortOrder == SortOrder.DESC ? reviewCount.desc() : reviewCount.asc();
      case RANDOM -> alcoholQuerySupporter.sortByRandom();
    };
  }

  /** 탐색 keyset에 쓰는 정렬 점수. RANDOM은 {@link AlcoholQuerySupporter#crc32Rank(long)} 를 쓴다. */
  public NumberExpression<? extends Number> sortScore(SearchSortType searchSortType) {
    NumberExpression<Double> avgRating = rating.ratingPoint.rating.avg().coalesce(0.0);
    NumberExpression<Long> reviewCount = review.id.countDistinct();
    NumberExpression<Long> pickCount = picks.id.countDistinct();
    return switch (searchSortType) {
      case POPULAR -> throw new IllegalArgumentException("POPULAR uses snapshot score");
      case RATING -> avgRating;
      case PICK -> pickCount;
      case REVIEW -> reviewCount;
      case RANDOM -> throw new IllegalArgumentException("RANDOM uses crc32Rank");
    };
  }

  /** 사용자가 주류에 준 평점 조회 (QueryDSL 경로 버전) */
  public NumberExpression<Double> myRating(NumberPath<Long> alcoholId, Long userId) {
    if (userId == null || userId == -1L) {
      return Expressions.asNumber(0.0).castToNum(Double.class).as("myRating");
    }
    return Expressions.asNumber(
            JPAExpressions.select(rating.ratingPoint.rating)
                .from(rating)
                .where(rating.id.alcoholId.eq(alcoholId).and(rating.id.userId.eq(userId)))
                .limit(1))
        .coalesce(0.0)
        .castToNum(Double.class)
        .as("myRating");
  }

  /** 사용자가 주류에 작성한 리뷰들의 평균 평점 계산 (QueryDSL 경로 버전) */
  public NumberExpression<Double> averageReviewRating(NumberPath<Long> alcoholId, Long userId) {
    if (userId == null || userId == -1L) {
      return Expressions.asNumber(0.0).castToNum(Double.class).as("myAvgRating");
    }
    return Expressions.asNumber(
            JPAExpressions.select(RatingDisplay.normalize(review.reviewRating.avg()).coalesce(0.0))
                .from(review)
                .where(
                    review
                        .alcoholId
                        .eq(alcoholId)
                        .and(review.userId.eq(userId))
                        .and(review.reviewRating.gt(0.0))))
        .castToNum(Double.class)
        .as("myAvgRating");
  }

  /** 특정 주류가 사용자에 의해 찜되었는지 확인 (QueryDSL 경로 버전) */
  public BooleanExpression isPickedSubquery(NumberPath<Long> alcoholId, Long userId) {
    if (userId == -1) {
      return Expressions.asBoolean(false);
    }
    return Expressions.asBoolean(
            JPAExpressions.selectOne()
                .from(picks)
                .where(
                    picks
                        .alcoholId
                        .eq(alcoholId)
                        .and(picks.userId.eq(userId))
                        .and(picks.status.eq(PICK)))
                .exists())
        .as("isPicked");
  }
}
