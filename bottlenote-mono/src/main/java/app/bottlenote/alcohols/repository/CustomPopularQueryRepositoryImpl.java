package app.bottlenote.alcohols.repository;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.alcohols.domain.QAlcoholPopularitySnapshot.alcoholPopularitySnapshot;
import static app.bottlenote.alcohols.domain.QAlcoholsTastingTags.alcoholsTastingTags;
import static app.bottlenote.picks.domain.QPicks.picks;
import static app.bottlenote.rating.domain.QRating.rating;

import app.bottlenote.alcohols.constant.BucketGranularity;
import app.bottlenote.alcohols.domain.QAlcoholPopularitySnapshot;
import app.bottlenote.alcohols.dto.response.PopularItem;
import app.bottlenote.picks.constant.PicksStatus;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
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
        alcoholPopularitySnapshot.popularityScore);
  }

  @Override
  public List<PopularItem> getPopularByInterestWeekly(Long userId, int limit) {
    return getPopularBySnapshot(
        userId, limit, BucketGranularity.WEEK, alcoholPopularitySnapshot.interestScore);
  }

  @Override
  public List<PopularItem> getPopularByInterestMonthly(Long userId, int limit) {
    return getPopularBySnapshot(
        userId, limit, BucketGranularity.MONTH, alcoholPopularitySnapshot.interestScore);
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
        .on(rating.id.alcoholId.eq(alcohol.id))
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

  private List<PopularItem> getPopularBySnapshot(
      Long userId, int limit, BucketGranularity granularity, NumberPath<BigDecimal> snapshotScore) {
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
        .on(rating.id.alcoholId.eq(alcohol.id))
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
            snapshotScore)
        .orderBy(snapshotScore.desc(), alcohol.id.asc())
        .limit(limit)
        .fetch();
  }

  private com.querydsl.core.types.ConstructorExpression<PopularItem> popularItemProjection(
      Long userId, NumberExpression<BigDecimal> snapshotScore) {
    return Projections.constructor(
        PopularItem.class,
        alcohol.id,
        alcohol.korName,
        alcohol.engName,
        displayedRating(),
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

  private NumberExpression<Double> displayedRating() {
    return rating
        .ratingPoint
        .rating
        .avg()
        .multiply(10)
        .castToNum(Double.class)
        .round()
        .divide(10)
        .coalesce(0.0);
  }
}
