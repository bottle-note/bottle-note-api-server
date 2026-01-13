package app.bottlenote.alcohols.repository;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.history.domain.QAlcoholsViewHistory.alcoholsViewHistory;
import static app.bottlenote.picks.domain.QPicks.picks;
import static app.bottlenote.rating.domain.QRating.rating;

import app.bottlenote.alcohols.dto.response.PopularItem;
import app.bottlenote.picks.constant.PicksStatus;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * 조회수 기반 인기 주류 조회를 위한 QueryDSL 구현체
 */
@RequiredArgsConstructor
public class CustomPopularQueryRepositoryImpl implements CustomPopularQueryRepository {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<PopularItem> getPopularByViewsWeekly(Long userId, int limit) {
    LocalDateTime weekStart = LocalDate.now()
        .with(DayOfWeek.MONDAY)
        .atStartOfDay();

    return getPopularByViews(userId, limit, weekStart);
  }

  @Override
  public List<PopularItem> getPopularByViewsMonthly(Long userId, int limit) {
    LocalDateTime monthStart = LocalDate.now()
        .withDayOfMonth(1)
        .atStartOfDay();

    return getPopularByViews(userId, limit, monthStart);
  }

  private List<PopularItem> getPopularByViews(Long userId, int limit, LocalDateTime startDate) {
    // 1. 조회수 기반 인기 주류 조회
    List<PopularItem> viewBasedResults = queryFactory
        .select(Projections.constructor(
            PopularItem.class,
            alcohol.id,
            alcohol.korName,
            alcohol.engName,
            rating.ratingPoint.rating.avg().coalesce(0.0),
            rating.id.alcoholId.count(),
            alcohol.korCategory,
            alcohol.engCategory,
            alcohol.imageUrl,
            ExpressionUtils.as(
                JPAExpressions
                    .selectOne()
                    .from(picks)
                    .where(
                        picks.alcoholId.eq(alcohol.id),
                        picks.userId.eq(userId),
                        picks.status.eq(PicksStatus.PICK))
                    .exists(),
                "isPicked"),
            alcoholsViewHistory.id.alcoholId.count().castToNum(Double.class)))
        .from(alcoholsViewHistory)
        .join(alcohol).on(alcoholsViewHistory.id.alcoholId.eq(alcohol.id))
        .leftJoin(rating).on(alcohol.id.eq(rating.id.alcoholId))
        .where(alcoholsViewHistory.viewAt.goe(startDate))
        .groupBy(
            alcohol.id,
            alcohol.korName,
            alcohol.engName,
            alcohol.korCategory,
            alcohol.engCategory,
            alcohol.imageUrl)
        .orderBy(alcoholsViewHistory.id.alcoholId.count().desc())
        .limit(limit)
        .fetch();

    // 2. 부족분은 평점 높은 주류로 채움
    if (viewBasedResults.size() < limit) {
      List<Long> excludeIds = viewBasedResults.stream()
          .map(PopularItem::alcoholId)
          .toList();

      int remaining = limit - viewBasedResults.size();

      List<PopularItem> ratingBasedResults = queryFactory
          .select(Projections.constructor(
              PopularItem.class,
              alcohol.id,
              alcohol.korName,
              alcohol.engName,
              rating.ratingPoint.rating.avg().coalesce(0.0),
              rating.id.alcoholId.count(),
              alcohol.korCategory,
              alcohol.engCategory,
              alcohol.imageUrl,
              ExpressionUtils.as(
                  JPAExpressions
                      .selectOne()
                      .from(picks)
                      .where(
                          picks.alcoholId.eq(alcohol.id),
                          picks.userId.eq(userId),
                          picks.status.eq(PicksStatus.PICK))
                      .exists(),
                  "isPicked"),
              Expressions.asNumber(0.0)))
          .from(alcohol)
          .join(rating).on(alcohol.id.eq(rating.id.alcoholId))
          .where(excludeIds.isEmpty()
              ? null
              : alcohol.id.notIn(excludeIds))
          .groupBy(
              alcohol.id,
              alcohol.korName,
              alcohol.engName,
              alcohol.korCategory,
              alcohol.engCategory,
              alcohol.imageUrl)
          .orderBy(rating.ratingPoint.rating.avg().desc())
          .limit(remaining)
          .fetch();

      List<PopularItem> result = new ArrayList<>(viewBasedResults);
      result.addAll(ratingBasedResults);
      return result;
    }

    return viewBasedResults;
  }
}
