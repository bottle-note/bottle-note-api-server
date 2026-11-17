package app.bottlenote.alcohols.repository;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.alcohols.domain.QAlcoholPopularitySnapshot.alcoholPopularitySnapshot;
import static app.bottlenote.alcohols.domain.QAlcoholsTastingTags.alcoholsTastingTags;
import static app.bottlenote.picks.domain.QPicks.picks;
import static app.bottlenote.rating.domain.QRating.rating;

import app.bottlenote.alcohols.constant.BucketGranularity;
import app.bottlenote.alcohols.domain.QAlcoholPopularitySnapshot;
import app.bottlenote.alcohols.dto.response.PopularItem;
import app.bottlenote.global.rating.RatingDisplay;
import app.bottlenote.picks.constant.PicksStatus;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

/** Snapshot 기반 인기 주류 조회 QueryDSL 구현체 */
@RequiredArgsConstructor
public class CustomPopularQueryRepositoryImpl implements CustomPopularQueryRepository {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<PopularItem> getPopularOfWeeks(Long userId, Pageable pageable) {
    return getPopularBySnapshot(
        userId,
        pageable.getPageSize(),
        BucketGranularity.WEEK,
        alcoholPopularitySnapshot.popularityScore,
        alcoholPopularitySnapshot.interestValue);
  }

  @Override
  public List<PopularItem> getPopularByInterestWeekly(Long userId, int limit) {
    return getPopularBySnapshot(
        userId,
        limit,
        BucketGranularity.WEEK,
        alcoholPopularitySnapshot.interestScore,
        alcoholPopularitySnapshot.interestValue);
  }

  @Override
  public List<PopularItem> getPopularByInterestMonthly(Long userId, int limit) {
    return getPopularBySnapshot(
        userId,
        limit,
        BucketGranularity.MONTH,
        alcoholPopularitySnapshot.interestScore,
        alcoholPopularitySnapshot.interestValue);
  }

  @Override
  public List<PopularItem> getSpringItems(
      Long userId, List<Long> tags, List<Long> excludedTags, Pageable pageable) {
    QAlcoholPopularitySnapshot latest = new QAlcoholPopularitySnapshot("latestSpringSnapshot");
    return queryFactory
        .select(popularItemProjection(userId, alcoholPopularitySnapshot.popularityScore))
        .from(alcoholPopularitySnapshot)
        .join(alcohol)
        .on(alcohol.id.eq(alcoholPopularitySnapshot.alcoholId))
        .join(alcoholsTastingTags)
        .on(alcoholsTastingTags.alcohol.id.eq(alcohol.id))
        .leftJoin(rating)
        .on(countedRating())
        .where(
            alcoholPopularitySnapshot.bucketGranularity.eq(BucketGranularity.WEEK),
            alcoholPopularitySnapshot.bucketAt.eq(
                JPAExpressions.select(latest.bucketAt.max())
                    .from(latest)
                    .where(latest.bucketGranularity.eq(BucketGranularity.WEEK))),
            alcoholsTastingTags.tastingTag.id.in(tags),
            alcoholsTastingTags.tastingTag.id.notIn(excludedTags),
            alcohol.deletedAt.isNull())
        .groupBy(
            alcohol.id,
            alcohol.korName,
            alcohol.engName,
            alcohol.korCategory,
            alcohol.engCategory,
            alcohol.imageUrl,
            alcoholPopularitySnapshot.popularityScore)
        .orderBy(
            com.querydsl.core.types.dsl.Expressions.numberTemplate(Double.class, "function('rand')")
                .asc())
        .limit(pageable.getPageSize())
        .fetch();
  }

  /**
   * 정규화 점수는 분모에 닿으면 만점으로 묶이므로 동점이 생긴다. 동점은 원시값 내림차순으로 풀고, 그래도 같으면 id로 고정한다.
   *
   * <p>종합 인기도의 동점은 기간마다 새로 쌓이는 조회수로 푼다. 나머지 축은 누적 상태라 주차가 바뀌어도 순서가 거의 바뀌지 않기 때문이다.
   */
  private List<PopularItem> getPopularBySnapshot(
      Long userId,
      int limit,
      BucketGranularity granularity,
      NumberPath<BigDecimal> snapshotScore,
      NumberPath<Long> tieBreakValue) {
    if (limit <= 0) {
      return List.of();
    }
    QAlcoholPopularitySnapshot latest = new QAlcoholPopularitySnapshot("latestPopularitySnapshot");
    return queryFactory
        .select(popularItemProjection(userId, snapshotScore))
        .from(alcoholPopularitySnapshot)
        .join(alcohol)
        .on(alcohol.id.eq(alcoholPopularitySnapshot.alcoholId))
        .leftJoin(rating)
        .on(countedRating())
        .where(
            alcoholPopularitySnapshot.bucketGranularity.eq(granularity),
            alcoholPopularitySnapshot.bucketAt.eq(
                JPAExpressions.select(latest.bucketAt.max())
                    .from(latest)
                    .where(latest.bucketGranularity.eq(granularity))),
            alcohol.deletedAt.isNull())
        .groupBy(
            alcohol.id,
            alcohol.korName,
            alcohol.engName,
            alcohol.korCategory,
            alcohol.engCategory,
            alcohol.imageUrl,
            snapshotScore,
            tieBreakValue)
        .orderBy(snapshotScore.desc(), tieBreakValue.desc(), alcohol.id.asc())
        .limit(limit)
        .fetch();
  }

  /** 노출 집계에 포함할 별점만 남기는 조인 조건. 0점은 평가로 보지 않는다. */
  private static BooleanExpression countedRating() {
    return rating.id.alcoholId.eq(alcohol.id).and(rating.ratingPoint.rating.gt(0.0));
  }

  private com.querydsl.core.types.ConstructorExpression<PopularItem> popularItemProjection(
      Long userId, NumberExpression<BigDecimal> snapshotScore) {
    return Projections.constructor(
        PopularItem.class,
        alcohol.id,
        alcohol.korName,
        alcohol.engName,
        RatingDisplay.normalize(rating.ratingPoint.rating.avg()).coalesce(0.0),
        rating.id.userId.countDistinct(),
        alcohol.korCategory,
        alcohol.engCategory,
        alcohol.imageUrl,
        ExpressionUtils.as(
            JPAExpressions.selectOne()
                .from(picks)
                .where(
                    picks.alcoholId.eq(alcohol.id),
                    picks.userId.eq(userId),
                    picks.status.eq(PicksStatus.PICK))
                .exists(),
            "isPicked"),
        snapshotScore.doubleValue());
  }
}
