package app.bottlenote.rating.repository;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.picks.domain.QPicks.picks;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.domain.QReview.review;

import app.bottlenote.global.pagination.CursorClaims;
import app.bottlenote.global.pagination.CursorKeys;
import app.bottlenote.global.pagination.HmacCursorCodec;
import app.bottlenote.global.pagination.KeysetPageResponse;
import app.bottlenote.global.pagination.KeysetPagination;
import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.picks.repository.PicksQuerySupporter;
import app.bottlenote.rating.constant.SearchSortType;
import app.bottlenote.rating.dto.dsl.RatingListFetchCriteria;
import app.bottlenote.rating.dto.response.RatingListFetchResponse;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CustomRatingQueryRepositoryImpl implements CustomRatingQueryRepository {

  private final RatingQuerySupporter ratingQuerySupporter;
  private final PicksQuerySupporter picksQuerySupporter;
  private final JPAQueryFactory queryFactory;

  private final HmacCursorCodec cursorCodec;

  public CustomRatingQueryRepositoryImpl(
      RatingQuerySupporter ratingQuerySupporter,
      PicksQuerySupporter picksQuerySupporter,
      JPAQueryFactory queryFactory,
      HmacCursorCodec cursorCodec) {
    this.ratingQuerySupporter = ratingQuerySupporter;
    this.picksQuerySupporter = picksQuerySupporter;
    this.queryFactory = queryFactory;
    this.cursorCodec = cursorCodec;
  }

  @Override
  public KeysetPageResponse<RatingListFetchResponse> fetchRatingList(RatingListFetchCriteria criteria) {
    Integer pageSize = criteria.size();
    Long userId = criteria.userId();
    int fetchSize = pageSize + 1;
    String context = criteria.context();

    List<RatingSeekKey> candidates = fetchCandidateIds(criteria, context, fetchSize);
    if (candidates.isEmpty()) {
      return KeysetPageResponse.of(
          RatingListFetchResponse.create(List.of()), new KeysetPagination(false, null));
    }

    List<Long> candidateIds = candidates.stream().map(RatingSeekKey::id).toList();
    Map<Long, String> sortById =
        candidates.stream().collect(Collectors.toMap(RatingSeekKey::id, RatingSeekKey::sortValue));

    // 후보 id로 단건 조회 - Info 필드는 집계값이 없어 조인/groupBy 불필요
    List<RatingListFetchResponse.Info> fetched =
        queryFactory
            .select(
                Projections.constructor(
                    RatingListFetchResponse.Info.class,
                    alcohol.id.as("alcoholId"),
                    alcohol.imageUrl.as("imageUrl"),
                    alcohol.korName.as("korName"),
                    alcohol.engName.as("engName"),
                    alcohol.korCategory.as("korCategoryName"),
                    alcohol.engCategory.as("engCategoryName"),
                    picksQuerySupporter.isPickedSubQuery(userId)))
            .from(alcohol)
            .where(alcohol.id.in(candidateIds))
            .fetch();

    Map<Long, RatingListFetchResponse.Info> byId =
        fetched.stream()
            .collect(
                Collectors.toMap(RatingListFetchResponse.Info::alcoholId, Function.identity()));
    List<RatingListFetchResponse.Info> ordered =
        candidateIds.stream().map(byId::get).filter(Objects::nonNull).toList();

    var slice =
        KeysetPagination.fromOverflow(
            ordered,
            pageSize,
            item -> {
              Map<String, String> keys =
                  Map.of(
                      "id", String.valueOf(item.alcoholId()),
                      "sort", sortById.getOrDefault(item.alcoholId(), "0"));
              Map<String, String> extra =
                  criteria.sortType() == SearchSortType.RANDOM
                      ? Map.of("seed", String.valueOf(criteria.seed()))
                      : Map.of();
              return cursorCodec.encode(context, keys, extra);
            });
    return KeysetPageResponse.of(RatingListFetchResponse.create(slice.items()), slice.pagination());
  }

  /** 1단계 후보 id 추출 - RANDOM은 CRC32 keyset, 나머지는 sortScore 집계 기반 keyset으로 정렬과 커서를 일치시킨다. */
  private List<RatingSeekKey> fetchCandidateIds(
      RatingListFetchCriteria criteria, String context, int fetchSize) {
    SearchSortType sortType = criteria.sortType();
    Long userId = criteria.userId();
    CursorClaims claims =
        criteria.cursor() == null ? null : cursorCodec.verify(criteria.cursor(), context);

    if (sortType == SearchSortType.RANDOM) {
      NumberExpression<Long> crc = ratingQuerySupporter.crc32Rank(criteria.seed());
      List<Tuple> rows =
          queryFactory
              .select(alcohol.id, crc)
              .from(alcohol)
              .leftJoin(rating)
              .on(alcohol.id.eq(rating.id.alcoholId))
              .leftJoin(picks)
              .on(alcohol.id.eq(picks.alcoholId))
              .leftJoin(review)
              .on(alcohol.id.eq(review.alcoholId))
              .where(
                  ratingQuerySupporter.eqAlcoholName(criteria.keyword()),
                  ratingQuerySupporter.eqAlcoholCategory(criteria.category()),
                  ratingQuerySupporter.eqAlcoholRegion(criteria.regionId()),
                  ratingQuerySupporter.neRatingByMe(userId),
                  randomSeek(claims, crc))
              .groupBy(alcohol.id)
              .orderBy(crc.asc(), alcohol.id.asc())
              .limit(fetchSize)
              .fetch();
      return toSeekKeys(rows, crc);
    }

    NumberExpression<? extends Number> sortScore = ratingQuerySupporter.sortScore(sortType);
    List<Tuple> rows =
        queryFactory
            .select(alcohol.id, sortScore)
            .from(alcohol)
            .leftJoin(rating)
            .on(alcohol.id.eq(rating.id.alcoholId))
            .leftJoin(picks)
            .on(alcohol.id.eq(picks.alcoholId))
            .leftJoin(review)
            .on(alcohol.id.eq(review.alcoholId))
            .where(
                ratingQuerySupporter.eqAlcoholName(criteria.keyword()),
                ratingQuerySupporter.eqAlcoholCategory(criteria.category()),
                ratingQuerySupporter.eqAlcoholRegion(criteria.regionId()),
                ratingQuerySupporter.neRatingByMe(userId))
            .groupBy(alcohol.id)
            .having(aggregateSeek(claims, sortType, criteria.sortOrder(), sortScore))
            .orderBy(ratingQuerySupporter.orderBy(sortType, criteria.sortOrder()), alcohol.id.asc())
            .limit(fetchSize)
            .fetch();
    return toSeekKeys(rows, sortScore);
  }

  private static List<RatingSeekKey> toSeekKeys(
      List<Tuple> rows, Expression<? extends Number> sortExpr) {
    return rows.stream()
        .map(
            row -> {
              Long id = row.get(alcohol.id);
              Number sort = row.get(sortExpr);
              return new RatingSeekKey(id, sort == null ? "0" : String.valueOf(sort));
            })
        .toList();
  }

  private static BooleanExpression randomSeek(CursorClaims claims, NumberExpression<Long> crc) {
    if (claims == null) {
      return null;
    }
    long lastCrc = CursorKeys.requireLong(claims, "sort");
    long lastId = CursorKeys.requireLong(claims, "id");
    return crc.gt(lastCrc).or(crc.eq(lastCrc).and(alcohol.id.gt(lastId)));
  }

  private static BooleanExpression aggregateSeek(
      CursorClaims claims,
      SearchSortType sortType,
      SortOrder sortOrder,
      NumberExpression<? extends Number> sortScore) {
    if (claims == null) {
      return null;
    }
    long lastId = CursorKeys.requireLong(claims, "id");
    boolean desc = sortOrder == SortOrder.DESC;
    if (sortType == SearchSortType.PICK || sortType == SearchSortType.REVIEW) {
      long last = CursorKeys.requireLong(claims, "sort");
      @SuppressWarnings("unchecked")
      NumberExpression<Long> expr = (NumberExpression<Long>) sortScore;
      BooleanExpression moved = desc ? expr.lt(last) : expr.gt(last);
      return moved.or(expr.eq(last).and(alcohol.id.gt(lastId)));
    }
    double last = CursorKeys.requireDouble(claims, "sort");
    @SuppressWarnings("unchecked")
    NumberExpression<Double> expr = (NumberExpression<Double>) sortScore;
    BooleanExpression moved = desc ? expr.lt(last) : expr.gt(last);
    return moved.or(expr.eq(last).and(alcohol.id.gt(lastId)));
  }

  private record RatingSeekKey(Long id, String sortValue) {}
}
