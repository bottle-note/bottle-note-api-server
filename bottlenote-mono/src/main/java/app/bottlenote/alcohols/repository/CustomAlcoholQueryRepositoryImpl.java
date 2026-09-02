package app.bottlenote.alcohols.repository;

import static app.bottlenote.alcohols.constant.AlcoholType.WHISKY;
import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.alcohols.domain.QAlcoholPopularitySnapshot.alcoholPopularitySnapshot;
import static app.bottlenote.alcohols.domain.QDistillery.distillery;
import static app.bottlenote.alcohols.domain.QRegion.region;
import static app.bottlenote.alcohols.repository.AlcoholQuerySupporter.getTastingTags;
import static app.bottlenote.picks.domain.QPicks.picks;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.domain.QReview.review;

import app.bottlenote.alcohols.constant.SearchSortType;
import app.bottlenote.alcohols.dto.dsl.ExploreStandardCriteria;
import app.bottlenote.alcohols.dto.request.AdminAlcoholSearchRequest;
import app.bottlenote.alcohols.dto.response.AdminAlcoholItem;
import app.bottlenote.alcohols.dto.response.AlcoholDetailItem;
import app.bottlenote.alcohols.dto.response.AlcoholLookupItem;
import app.bottlenote.alcohols.dto.response.CategoryItem;
import app.bottlenote.alcohols.facade.payload.AlcoholMatchTargetItem;
import app.bottlenote.alcohols.facade.payload.AlcoholSummaryItem;
import app.bottlenote.global.pagination.CursorClaims;
import app.bottlenote.global.pagination.CursorKeys;
import app.bottlenote.global.pagination.HmacCursorCodec;
import app.bottlenote.global.pagination.KeysetPageResponse;
import app.bottlenote.global.pagination.KeysetPagination;
import app.bottlenote.global.service.cursor.SortOrder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

public class CustomAlcoholQueryRepositoryImpl implements CustomAlcoholQueryRepository {
  private final JPAQueryFactory queryFactory;
  private final AlcoholQuerySupporter supporter;
  private final HmacCursorCodec cursorCodec;

  public CustomAlcoholQueryRepositoryImpl(
      JPAQueryFactory queryFactory, AlcoholQuerySupporter supporter, HmacCursorCodec cursorCodec) {
    this.queryFactory = queryFactory;
    this.supporter = supporter;
    this.cursorCodec = cursorCodec;
  }

  /** 모든 카테고리(한글, 영문, 그룹) 조회 — 카테고리 레퍼런스 응답용 */
  @Override
  public List<CategoryItem> findAllCategoryItems() {
    return queryFactory
        .select(
            Projections.constructor(
                CategoryItem.class,
                alcohol.korCategory,
                alcohol.engCategory,
                alcohol.categoryGroup))
        .from(alcohol)
        .where(alcohol.type.eq(WHISKY), supporter.isNotDeleted())
        .groupBy(alcohol.korCategory, alcohol.engCategory, alcohol.categoryGroup)
        .orderBy(alcohol.korCategory.asc())
        .fetch();
  }

  @Override
  public List<AlcoholLookupItem> findAllLookupItems() {
    return queryFactory
        .select(
            Projections.constructor(
                AlcoholLookupItem.class,
                alcohol.id,
                alcohol.korName,
                alcohol.engName,
                alcohol.korCategory,
                alcohol.engCategory,
                alcohol.categoryGroup,
                region.id,
                region.korName,
                region.engName,
                distillery.id,
                distillery.korName,
                distillery.engName,
                alcohol.imageUrl))
        .from(alcohol)
        .leftJoin(region)
        .on(alcohol.region.id.eq(region.id))
        .leftJoin(distillery)
        .on(alcohol.distillery.id.eq(distillery.id))
        .where(alcohol.type.eq(WHISKY), alcohol.deletedAt.isNull())
        .orderBy(alcohol.id.asc())
        .fetch();
  }

  @Override
  public List<AlcoholMatchTargetItem> findAllMatchTargets() {
    return queryFactory
        .select(matchTargetProjection())
        .from(alcohol)
        .leftJoin(region)
        .on(alcohol.region.id.eq(region.id))
        .leftJoin(distillery)
        .on(alcohol.distillery.id.eq(distillery.id))
        .where(alcohol.deletedAt.isNull())
        .orderBy(alcohol.id.asc())
        .fetch();
  }

  @Override
  public List<AlcoholMatchTargetItem> findMatchTargetsByDistilleryIdIn(List<Long> distilleryIds) {
    if (distilleryIds == null || distilleryIds.isEmpty()) {
      return List.of();
    }
    return queryFactory
        .select(matchTargetProjection())
        .from(alcohol)
        .leftJoin(region)
        .on(alcohol.region.id.eq(region.id))
        .leftJoin(distillery)
        .on(alcohol.distillery.id.eq(distillery.id))
        .where(alcohol.distillery.id.in(distilleryIds), alcohol.deletedAt.isNull())
        .orderBy(alcohol.id.asc())
        .fetch();
  }

  @Override
  public List<AlcoholMatchTargetItem> findMatchTargetsByIdIn(List<Long> alcoholIds) {
    return queryFactory
        .select(matchTargetProjection())
        .from(alcohol)
        .leftJoin(region)
        .on(alcohol.region.id.eq(region.id))
        .leftJoin(distillery)
        .on(alcohol.distillery.id.eq(distillery.id))
        .where(alcohol.id.in(alcoholIds), alcohol.deletedAt.isNull())
        .orderBy(alcohol.id.asc())
        .fetch();
  }

  private Expression<AlcoholMatchTargetItem> matchTargetProjection() {
    return Projections.constructor(
        AlcoholMatchTargetItem.class,
        alcohol.id,
        alcohol.korName,
        alcohol.engName,
        alcohol.abv,
        alcohol.age,
        alcohol.korCategory,
        alcohol.engCategory,
        region.id,
        region.korName,
        region.engName,
        distillery.id,
        distillery.korName,
        distillery.engName,
        alcohol.imageUrl,
        alcohol.volume);
  }

  /** queryDSL 알코올 상세 조회 */
  @Override
  public AlcoholDetailItem findAlcoholDetailById(Long alcoholId, Long userId) {
    if (Objects.isNull(userId)) userId = -1L;

    return queryFactory
        .select(
            Projections.constructor(
                AlcoholDetailItem.class,
                alcohol.id,
                alcohol.imageUrl,
                alcohol.korName,
                alcohol.engName,
                alcohol.korCategory,
                alcohol.engCategory,
                region.korName,
                region.engName,
                alcohol.cask,
                alcohol.abv,
                distillery.korName,
                distillery.engName,
                displayedRating().as("rating"),
                rating.id.countDistinct(),
                supporter.myRating(alcoholId, userId),
                supporter.userReviewRating(alcoholId, userId),
                supporter.isPickedSubquery(alcoholId, userId),
                review.id.countDistinct(),
                picks.id.countDistinct(),
                getTastingTags()))
        .from(alcohol)
        .leftJoin(rating)
        .on(rating.id.alcoholId.eq(alcohol.id))
        .leftJoin(review)
        .on(review.alcoholId.eq(alcohol.id))
        .leftJoin(picks)
        .on(picks.alcoholId.eq(alcohol.id))
        .join(region)
        .on(alcohol.region.id.eq(region.id))
        .join(distillery)
        .on(alcohol.distillery.id.eq(distillery.id))
        .where(alcohol.id.eq(alcoholId), supporter.isNotDeleted())
        .groupBy(
            alcohol.id,
            alcohol.imageUrl,
            alcohol.korName,
            alcohol.engName,
            alcohol.korCategory,
            alcohol.engCategory,
            region.korName,
            region.engName,
            alcohol.cask,
            alcohol.abv,
            distillery.korName,
            distillery.engName)
        .fetchOne();
  }

  /** queryDSL 리뷰 상세 조회 시 포함 될 술의 정보를 조회합니다. */
  @Override
  public Optional<AlcoholSummaryItem> findAlcoholInfoById(Long alcoholId, Long userId) {

    return Optional.ofNullable(
        queryFactory
            .select(
                Projections.constructor(
                    AlcoholSummaryItem.class,
                    alcohol.id.as("alcoholId"),
                    alcohol.korName.as("korName"),
                    alcohol.engName.as("engName"),
                    alcohol.korCategory.as("korCategoryName"),
                    alcohol.engCategory.as("engCategoryName"),
                    alcohol.imageUrl.as("imageUrl"),
                    supporter.isPickedSubquery(alcoholId, userId)))
            .from(alcohol)
            .leftJoin(rating)
            .on(alcohol.id.eq(rating.id.alcoholId))
            .join(region)
            .on(alcohol.region.id.eq(region.id))
            .join(distillery)
            .on(alcohol.distillery.id.eq(distillery.id))
            .where(alcohol.id.eq(alcoholId), supporter.isNotDeleted())
            .groupBy(
                alcohol.id,
                alcohol.korCategory,
                alcohol.engCategory,
                alcohol.imageUrl,
                alcohol.korName,
                alcohol.engName,
                region.korName,
                region.engName,
                alcohol.cask,
                alcohol.abv,
                distillery.korName,
                distillery.engName)
            .fetchOne());
  }

  /** queryDSL 알코올 둘러보기 */
  @Override
  public KeysetPageResponse<List<AlcoholDetailItem>> getStandardExplore(
      ExploreStandardCriteria criteria) {
    Long userId = criteria.userId();
    int pageSize = criteria.size();
    int fetchSize = Math.addExact(pageSize, 1);
    String context = criteria.context();

    List<ExploreSeekKey> candidates = fetchCandidateIds(criteria, fetchSize);
    if (candidates.isEmpty()) {
      return KeysetPageResponse.of(List.of(), new KeysetPagination(false, null));
    }

    List<Long> candidateIds = candidates.stream().map(ExploreSeekKey::id).toList();
    Map<Long, String> sortById =
        candidates.stream()
            .collect(Collectors.toMap(ExploreSeekKey::id, ExploreSeekKey::sortValue));

    List<AlcoholDetailItem> items =
        queryFactory
            .select(
                Projections.constructor(
                    AlcoholDetailItem.class,
                    alcohol.id,
                    alcohol.imageUrl,
                    alcohol.korName,
                    alcohol.engName,
                    alcohol.korCategory,
                    alcohol.engCategory,
                    region.korName,
                    region.engName,
                    alcohol.cask,
                    alcohol.abv,
                    distillery.korName,
                    distillery.engName,
                    displayedRating().as("rating"),
                    rating.id.countDistinct(),
                    supporter.myRating(alcohol.id, userId),
                    supporter.userReviewRating(alcohol.id, userId),
                    supporter.isPickedSubquery(alcohol.id, userId),
                    review.id.countDistinct(),
                    picks.id.countDistinct(),
                    getTastingTags()))
            .from(alcohol)
            .leftJoin(rating)
            .on(rating.id.alcoholId.eq(alcohol.id))
            .leftJoin(review)
            .on(review.alcoholId.eq(alcohol.id))
            .leftJoin(picks)
            .on(picks.alcoholId.eq(alcohol.id))
            .join(region)
            .on(alcohol.region.id.eq(region.id))
            .join(distillery)
            .on(alcohol.distillery.id.eq(distillery.id))
            .where(alcohol.id.in(candidateIds), supporter.isNotDeleted())
            .groupBy(
                alcohol.id,
                alcohol.imageUrl,
                alcohol.korName,
                alcohol.engName,
                alcohol.korCategory,
                alcohol.engCategory,
                region.korName,
                region.engName,
                alcohol.cask,
                alcohol.abv,
                distillery.korName,
                distillery.engName)
            .fetch();

    Map<Long, AlcoholDetailItem> byId =
        items.stream()
            .collect(Collectors.toMap(AlcoholDetailItem::getAlcoholId, Function.identity()));
    List<AlcoholDetailItem> ordered =
        candidateIds.stream().map(byId::get).filter(Objects::nonNull).toList();

    var slice =
        KeysetPagination.fromOverflow(
            ordered,
            pageSize,
            item -> {
              Map<String, String> keys =
                  Map.of(
                      "id",
                      String.valueOf(item.getAlcoholId()),
                      "sort",
                      sortById.getOrDefault(item.getAlcoholId(), "0"));
              Map<String, String> extra =
                  switch (criteria.sortType()) {
                    case RANDOM -> Map.of("seed", String.valueOf(criteria.seed()));
                    case POPULAR -> Map.of("bucketAt", criteria.popularityBucketAt().toString());
                    default -> Map.of();
                  };
              return cursorCodec.encode(context, keys, extra);
            });
    return KeysetPageResponse.of(slice.items(), slice.pagination());
  }

  /**
   * 1단계 후보 ID 추출. 정렬 타입에 따라 RANDOM은 CRC32 keyset, 나머지는 필요한 집계 테이블만 LEFT JOIN + GROUP BY + id ASC 보조
   * 정렬로 처리한다.
   */
  private List<ExploreSeekKey> fetchCandidateIds(ExploreStandardCriteria criteria, int fetchSize) {
    SearchSortType sortType = criteria.sortType();
    CursorClaims claims =
        criteria.cursor() == null
            ? null
            : cursorCodec.verify(criteria.cursor(), criteria.context());

    if (sortType == SearchSortType.POPULAR) {
      return fetchPopularityCandidates(criteria, claims, fetchSize);
    }

    if (sortType == SearchSortType.RANDOM) {
      NumberExpression<Long> crc = supporter.crc32Rank(criteria.seed());
      if (criteria.hasRatingRange()) {
        List<Tuple> rows =
            queryFactory
                .select(alcohol.id, crc)
                .from(alcohol)
                .join(region)
                .on(alcohol.region.id.eq(region.id))
                .join(distillery)
                .on(alcohol.distillery.id.eq(distillery.id))
                .leftJoin(rating)
                .on(rating.id.alcoholId.eq(alcohol.id))
                .where(
                    supporter.searchTokensMatch(criteria.searchTokens()),
                    supporter.eqCategory(criteria.category()),
                    supporter.inRegionIds(criteria.regionIds()),
                    supporter.inDistilleryIds(criteria.distilleryIds()),
                    supporter.eqCurationId(criteria.curationId()),
                    supporter.isNotDeleted(),
                    randomSeek(claims, crc))
                .groupBy(alcohol.id)
                .having(ratingInRange(criteria.ratingFrom(), criteria.ratingTo()))
                .orderBy(crc.asc(), alcohol.id.asc())
                .limit(fetchSize)
                .fetch();
        return toSeekKeys(rows, crc);
      }
      List<Tuple> rows =
          queryFactory
              .select(alcohol.id, crc)
              .from(alcohol)
              .join(region)
              .on(alcohol.region.id.eq(region.id))
              .join(distillery)
              .on(alcohol.distillery.id.eq(distillery.id))
              .where(
                  supporter.searchTokensMatch(criteria.searchTokens()),
                  supporter.eqCategory(criteria.category()),
                  supporter.inRegionIds(criteria.regionIds()),
                  supporter.inDistilleryIds(criteria.distilleryIds()),
                  supporter.eqCurationId(criteria.curationId()),
                  supporter.isNotDeleted(),
                  randomSeek(claims, crc))
              .orderBy(crc.asc(), alcohol.id.asc())
              .limit(fetchSize)
              .fetch();
      return toSeekKeys(rows, crc);
    }

    NumberExpression<? extends Number> sortScore = supporter.sortScore(sortType);
    var query =
        queryFactory
            .select(alcohol.id, sortScore)
            .from(alcohol)
            .join(region)
            .on(alcohol.region.id.eq(region.id))
            .join(distillery)
            .on(alcohol.distillery.id.eq(distillery.id));
    if (needsRatingJoin(sortType) || criteria.hasRatingRange()) {
      query = query.leftJoin(rating).on(rating.id.alcoholId.eq(alcohol.id));
    }
    if (needsReviewJoin(sortType)) {
      query = query.leftJoin(review).on(review.alcoholId.eq(alcohol.id));
    }
    if (needsPicksJoin(sortType)) {
      query = query.leftJoin(picks).on(picks.alcoholId.eq(alcohol.id));
    }
    List<Tuple> rows =
        query
            .where(
                supporter.searchTokensMatch(criteria.searchTokens()),
                supporter.eqCategory(criteria.category()),
                supporter.inRegionIds(criteria.regionIds()),
                supporter.inDistilleryIds(criteria.distilleryIds()),
                supporter.eqCurationId(criteria.curationId()),
                supporter.isNotDeleted())
            .groupBy(alcohol.id)
            .having(
                ratingInRange(criteria.ratingFrom(), criteria.ratingTo()),
                aggregateSeek(claims, sortType, criteria.sortOrder(), sortScore))
            .orderBy(supporter.sortBy(sortType, criteria.sortOrder()), alcohol.id.asc())
            .limit(fetchSize)
            .fetch();
    return toSeekKeys(rows, sortScore);
  }

  private List<ExploreSeekKey> fetchPopularityCandidates(
      ExploreStandardCriteria criteria, CursorClaims claims, int fetchSize) {
    if (criteria.popularityBucketAt() == null) {
      return List.of();
    }

    NumberExpression<BigDecimal> score = alcoholPopularitySnapshot.popularityScore;
    var query =
        queryFactory
            .select(alcohol.id, score)
            .from(alcohol)
            .join(region)
            .on(alcohol.region.id.eq(region.id))
            .join(distillery)
            .on(alcohol.distillery.id.eq(distillery.id))
            .join(alcoholPopularitySnapshot)
            .on(
                alcoholPopularitySnapshot
                    .alcoholId
                    .eq(alcohol.id)
                    .and(
                        alcoholPopularitySnapshot.bucketGranularity.eq(
                            app.bottlenote.alcohols.constant.BucketGranularity.HOUR))
                    .and(alcoholPopularitySnapshot.bucketAt.eq(criteria.popularityBucketAt())));
    if (criteria.hasRatingRange()) {
      query = query.leftJoin(rating).on(rating.id.alcoholId.eq(alcohol.id));
    }

    OrderSpecifier<BigDecimal> scoreOrder =
        criteria.sortOrder() == SortOrder.DESC ? score.desc() : score.asc();
    List<Tuple> rows =
        query
            .where(
                supporter.searchTokensMatch(criteria.searchTokens()),
                supporter.eqCategory(criteria.category()),
                supporter.inRegionIds(criteria.regionIds()),
                supporter.inDistilleryIds(criteria.distilleryIds()),
                supporter.eqCurationId(criteria.curationId()),
                supporter.isNotDeleted(),
                popularitySeek(claims, criteria.sortOrder(), score))
            .groupBy(alcohol.id, score)
            .having(ratingInRange(criteria.ratingFrom(), criteria.ratingTo()))
            .orderBy(scoreOrder, alcohol.id.asc())
            .limit(fetchSize)
            .fetch();
    return toSeekKeys(rows, score);
  }

  private static List<ExploreSeekKey> toSeekKeys(
      List<Tuple> rows, Expression<? extends Number> sortExpr) {
    return rows.stream()
        .map(
            row -> {
              Long id = row.get(alcohol.id);
              Number sort = row.get(sortExpr);
              return new ExploreSeekKey(id, sort == null ? "0" : String.valueOf(sort));
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

  private static BooleanExpression popularitySeek(
      CursorClaims claims, SortOrder sortOrder, NumberExpression<BigDecimal> score) {
    if (claims == null) {
      return null;
    }
    BigDecimal lastScore = CursorKeys.requireBigDecimal(claims, "sort");
    long lastId = CursorKeys.requireLong(claims, "id");
    BooleanExpression moved =
        sortOrder == SortOrder.DESC ? score.lt(lastScore) : score.gt(lastScore);
    return moved.or(score.eq(lastScore).and(alcohol.id.gt(lastId)));
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

  private record ExploreSeekKey(Long id, String sortValue) {}

  /** 목록과 상세 응답에 노출하는 집계 평점을 소수점 첫째 자리로 반올림한다. */
  private static NumberExpression<Double> displayedRating() {
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

  /** 기존 별점 범위 필터의 0.5 단위 반올림 계약을 유지한다. */
  private static NumberExpression<Double> filterRating() {
    return rating
        .ratingPoint
        .rating
        .avg()
        .multiply(2)
        .castToNum(Double.class)
        .round()
        .divide(2)
        .coalesce(0.0);
  }

  private static BooleanExpression ratingInRange(BigDecimal from, BigDecimal to) {
    BooleanExpression lower = from == null ? null : filterRating().goe(from.doubleValue());
    BooleanExpression upper = to == null ? null : filterRating().loe(to.doubleValue());
    BooleanExpression hasRating = rating.id.count().gt(0L);
    if (lower == null) {
      return upper == null ? null : hasRating.and(upper);
    }
    return hasRating.and(upper == null ? lower : lower.and(upper));
  }

  private static boolean needsRatingJoin(SearchSortType sortType) {
    return sortType == SearchSortType.RATING;
  }

  private static boolean needsReviewJoin(SearchSortType sortType) {
    return sortType == SearchSortType.REVIEW;
  }

  private static boolean needsPicksJoin(SearchSortType sortType) {
    return sortType == SearchSortType.PICK;
  }

  /** Admin용 알코올 검색 (Offset 페이징) */
  @Override
  public Page<AdminAlcoholItem> searchAdminAlcohols(AdminAlcoholSearchRequest request) {
    List<AdminAlcoholItem> content =
        queryFactory
            .select(
                Projections.constructor(
                    AdminAlcoholItem.class,
                    alcohol.id,
                    alcohol.korName,
                    alcohol.engName,
                    alcohol.korCategory,
                    alcohol.engCategory,
                    alcohol.imageUrl,
                    alcohol.createAt,
                    alcohol.lastModifyAt,
                    alcohol.deletedAt))
            .from(alcohol)
            .where(
                supporter.keywordMatch(request.keyword()),
                supporter.eqCategory(request.category()),
                supporter.eqRegion(request.regionId()),
                Boolean.TRUE.equals(request.includeDeleted()) ? null : supporter.isNotDeleted())
            .orderBy(supporter.sortByAdmin(request.sortType(), request.sortOrder()))
            .offset((long) request.page() * request.size())
            .limit(request.size())
            .fetch();

    Long total =
        queryFactory
            .select(alcohol.id.count())
            .from(alcohol)
            .where(
                supporter.keywordMatch(request.keyword()),
                supporter.eqCategory(request.category()),
                supporter.eqRegion(request.regionId()),
                Boolean.TRUE.equals(request.includeDeleted()) ? null : supporter.isNotDeleted())
            .fetchOne();

    return new PageImpl<>(
        content, PageRequest.of(request.page(), request.size()), total != null ? total : 0L);
  }

  /** Admin용 알코올 단건 상세 조회 */
  @Override
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
                    rating
                        .ratingPoint
                        .rating
                        .avg()
                        .multiply(2)
                        .castToNum(Double.class)
                        .round()
                        .divide(2)
                        .coalesce(0.0),
                    rating.id.count(),
                    review.id.countDistinct(),
                    picks.id.countDistinct(),
                    alcohol.createAt,
                    alcohol.lastModifyAt))
            .from(alcohol)
            .leftJoin(rating)
            .on(rating.id.alcoholId.eq(alcohol.id))
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
}
