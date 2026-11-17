package app.bottlenote.alcohols.repository;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.alcohols.domain.QDistillery.distillery;
import static app.bottlenote.alcohols.domain.QRegion.region;
import static app.bottlenote.picks.domain.QPicks.picks;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.domain.QReview.review;

import app.bottlenote.global.rating.RatingDisplay;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** 어드민 주류 단건 상세 조회 구현. 별점·리뷰·찜을 조인하므로 최종 소유는 admin-api다. */
@Repository
public class QueryDslAdminAlcoholDetailRepository {
  private final JPAQueryFactory queryFactory;

  public QueryDslAdminAlcoholDetailRepository(JPAQueryFactory queryFactory) {
    this.queryFactory = queryFactory;
  }

  /** Admin용 알코올 단건 상세 조회 */
  public Optional<AdminAlcoholDetailProjection> findAdminAlcoholDetailById(Long alcoholId) {
    AdminAlcoholDetailProjection result =
        queryFactory
            .select(
                Projections.constructor(
                    AdminAlcoholDetailProjection.class,
                    alcohol.id,
                    alcohol.korName,
                    alcohol.engName,
                    alcohol.imageUrl,
                    alcohol.type.stringValue(),
                    alcohol.korCategory,
                    alcohol.engCategory,
                    alcohol.categoryGroup.stringValue(),
                    alcohol.abv,
                    alcohol.age,
                    alcohol.cask,
                    alcohol.volume,
                    alcohol.description,
                    region.id,
                    region.korName,
                    region.engName,
                    distillery.id,
                    distillery.korName,
                    distillery.engName,
                    displayedRating(),
                    rating.id.count(),
                    review.id.countDistinct(),
                    picks.id.countDistinct(),
                    alcohol.createAt,
                    alcohol.lastModifyAt))
            .from(alcohol)
            .leftJoin(rating)
            .on(countedRating())
            .leftJoin(review)
            .on(review.alcoholId.eq(alcohol.id))
            .leftJoin(picks)
            .on(picks.alcoholId.eq(alcohol.id))
            .leftJoin(region)
            .on(alcohol.region.id.eq(region.id))
            .leftJoin(distillery)
            .on(alcohol.distillery.id.eq(distillery.id))
            .where(alcohol.id.eq(alcoholId))
            .groupBy(
                alcohol.id,
                alcohol.korName,
                alcohol.engName,
                alcohol.imageUrl,
                alcohol.type,
                alcohol.korCategory,
                alcohol.engCategory,
                alcohol.categoryGroup,
                alcohol.abv,
                alcohol.age,
                alcohol.cask,
                alcohol.volume,
                alcohol.description,
                region.id,
                region.korName,
                region.engName,
                distillery.id,
                distillery.korName,
                distillery.engName,
                alcohol.createAt,
                alcohol.lastModifyAt)
            .fetchOne();

    return Optional.ofNullable(result);
  }

  /** 노출 집계에 포함할 별점만 남기는 조인 조건. 0점은 평가로 보지 않는다. */
  private static BooleanExpression countedRating() {
    return rating.id.alcoholId.eq(alcohol.id).and(rating.ratingPoint.rating.gt(0.0));
  }

  /** 목록 응답에 노출하는 집계 평점과 같은 정규화 값을 HAVING과 projection에서 공통 사용한다. */
  private static NumberExpression<Double> displayedRating() {
    return RatingDisplay.normalize(rating.ratingPoint.rating.avg()).coalesce(0.0);
  }

  public record AdminAlcoholDetailProjection(
      Long alcoholId,
      String korName,
      String engName,
      String imageUrl,
      String type,
      String korCategory,
      String engCategory,
      String categoryGroup,
      String abv,
      String age,
      String cask,
      String volume,
      String description,
      Long regionId,
      String korRegion,
      String engRegion,
      Long distilleryId,
      String korDistillery,
      String engDistillery,
      Double avgRating,
      Long totalRatingsCount,
      Long reviewCount,
      Long pickCount,
      LocalDateTime createdAt,
      LocalDateTime modifiedAt) {}
}
