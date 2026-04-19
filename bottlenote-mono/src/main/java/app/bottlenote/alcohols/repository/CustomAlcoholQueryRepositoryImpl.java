package app.bottlenote.alcohols.repository;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.alcohols.domain.QDistillery.distillery;
import static app.bottlenote.alcohols.domain.QRegion.region;
import static app.bottlenote.alcohols.repository.AlcoholQuerySupporter.getTastingTags;
import static app.bottlenote.picks.domain.QPicks.picks;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.domain.QReview.review;

import app.bottlenote.alcohols.constant.SearchSortType;
import app.bottlenote.alcohols.dto.dsl.AlcoholSearchCriteria;
import app.bottlenote.alcohols.dto.dsl.ExploreStandardCriteria;
import app.bottlenote.alcohols.dto.request.AdminAlcoholSearchRequest;
import app.bottlenote.alcohols.dto.response.AdminAlcoholItem;
import app.bottlenote.alcohols.dto.response.AlcoholDetailItem;
import app.bottlenote.alcohols.dto.response.AlcoholSearchResponse;
import app.bottlenote.alcohols.dto.response.AlcoholsSearchItem;
import app.bottlenote.alcohols.facade.payload.AlcoholSummaryItem;
import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.CursorResponse;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.global.service.cursor.SortOrder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@AllArgsConstructor
public class CustomAlcoholQueryRepositoryImpl implements CustomAlcoholQueryRepository {
  private final JPAQueryFactory queryFactory;
  private final AlcoholQuerySupporter supporter;

  /** 모든 카테고리 페어(한글, 영문) 조회 */
  @Override
  public List<Pair<String, String>> findAllCategoryPairs() {
    return queryFactory
        .select(alcohol.korCategory, alcohol.engCategory)
        .from(alcohol)
        .groupBy(alcohol.korCategory, alcohol.engCategory)
        .orderBy(alcohol.korCategory.asc())
        .fetch()
        .stream()
        .map(tuple -> Pair.of(tuple.get(alcohol.korCategory), tuple.get(alcohol.engCategory)))
        .toList();
  }

  /** queryDSL 알코올 검색 */
  @Override
  public PageResponse<AlcoholSearchResponse> searchAlcohols(AlcoholSearchCriteria criteriaDto) {
    Long cursor = criteriaDto.cursor();
    Long pageSize = criteriaDto.pageSize();
    SearchSortType sortType = criteriaDto.sortType();
    SortOrder sortOrder = criteriaDto.sortOrder();

    Long userId = criteriaDto.userId();

    List<AlcoholsSearchItem> fetch =
        queryFactory
            .select(
                Projections.fields(
                    AlcoholsSearchItem.class,
                    alcohol.id.as("alcoholId"),
                    alcohol.korName.as("korName"),
                    alcohol.engName.as("engName"),
                    alcohol.korCategory.as("korCategoryName"),
                    alcohol.engCategory.as("engCategoryName"),
                    alcohol.imageUrl.as("imageUrl"),
                    rating
                        .ratingPoint
                        .rating
                        .avg()
                        .multiply(2)
                        .castToNum(Double.class)
                        .round()
                        .divide(2)
                        .coalesce(0.0)
                        .as("rating"),
                    rating.id.countDistinct().as("ratingCount"),
                    review.id.countDistinct().as("reviewCount"),
                    picks.id.countDistinct().as("pickCount"),
                    supporter.pickedSubQuery(userId).as("isPicked")))
            .from(alcohol)
            .leftJoin(rating)
            .on(alcohol.id.eq(rating.id.alcoholId))
            .leftJoin(picks)
            .on(alcohol.id.eq(picks.alcoholId))
            .leftJoin(review)
            .on(alcohol.id.eq(review.alcoholId))
            .where(
                supporter.keywordMatch(criteriaDto.keyword()),
                supporter.eqCurationId(criteriaDto.curationId()),
                supporter.eqCategory(criteriaDto.category()),
                supporter.eqRegion(criteriaDto.regionId()))
            .groupBy(
                alcohol.id,
                alcohol.korName,
                alcohol.engName,
                alcohol.korCategory,
                alcohol.engCategory,
                alcohol.imageUrl)
            .orderBy(supporter.sortBy(sortType, sortOrder))
            .orderBy(alcohol.id.asc())
            .offset(cursor)
            .limit(criteriaDto.pageSize() + 1) // 다음 페이지가 있는지 확인하기 위해 1개 더 가져옴
            .fetch();

    // where 조건으로 전체 결과값 카운트
    Long totalCount =
        queryFactory
            .select(alcohol.id.count())
            .from(alcohol)
            .where(
                supporter.keywordMatch(criteriaDto.keyword()),
                supporter.eqCurationId(criteriaDto.curationId()),
                supporter.eqCategory(criteriaDto.category()),
                supporter.eqRegion(criteriaDto.regionId()))
            .fetchOne();

    CursorPageable pageable = CursorPageable.of(fetch, cursor, pageSize);
    List<AlcoholsSearchItem> content =
        fetch.size() > pageSize ? fetch.subList(0, pageSize.intValue()) : fetch;
    return PageResponse.of(AlcoholSearchResponse.of(totalCount, content), pageable);
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
                rating
                    .ratingPoint
                    .rating
                    .avg()
                    .multiply(2)
                    .castToNum(Double.class)
                    .round()
                    .divide(2)
                    .coalesce(0.0)
                    .as("rating"),
                rating.id.countDistinct(),
                supporter.myRating(alcoholId, userId),
                supporter.averageReviewRating(alcoholId, userId),
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
        .where(alcohol.id.eq(alcoholId))
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
            .where(alcohol.id.eq(alcoholId))
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
  public CursorResponse<AlcoholDetailItem> getStandardExplore(ExploreStandardCriteria criteria) {
    Long userId = criteria.userId();
    Long cursor = criteria.cursor();
    int pageSize = criteria.size();
    int fetchSize = pageSize + 1;

    // 1단계: 후보 alcohol.id 만 추출. 정렬 타입별로 조인/집계 수준을 분기하여 성능을 보존한다.
    // heavy 상관 서브쿼리(myRating/averageReviewRating/isPickedSubquery/getTastingTags)는
    // 반드시 2단계에서만 실행되어야 한다 (성능개선 이슈 참고).
    List<Long> candidateIds = fetchCandidateIds(criteria, fetchSize);

    // 빈 결과 early return (IN 절 빈 리스트 방지)
    if (candidateIds.isEmpty()) {
      return CursorResponse.of(List.of(), cursor, pageSize);
    }

    // 2단계: 1단계 ID 들에 대해서만 본문 + 평점 + 태그 조회 (3,000건 처리 → fetchSize 건 처리)
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
                    rating
                        .ratingPoint
                        .rating
                        .avg()
                        .multiply(2)
                        .castToNum(Double.class)
                        .round()
                        .divide(2)
                        .coalesce(0.0)
                        .as("rating"),
                    rating.id.countDistinct(),
                    supporter.myRating(alcohol.id, userId),
                    supporter.averageReviewRating(alcohol.id, userId),
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
            .where(alcohol.id.in(candidateIds))
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

    // 1단계의 ORDER BY rand 순서를 앱 레벨에서 복원 (2단계 SQL 결과는 임의 순서)
    Map<Long, AlcoholDetailItem> byId =
        items.stream()
            .collect(Collectors.toMap(AlcoholDetailItem::getAlcoholId, Function.identity()));
    List<AlcoholDetailItem> ordered =
        candidateIds.stream().map(byId::get).filter(Objects::nonNull).toList();

    return CursorResponse.of(ordered, cursor, pageSize);
  }

  /**
   * 1단계 후보 ID 추출. 정렬 타입에 따라 RANDOM은 경량 경로, 나머지는 필요한 집계 테이블만 LEFT JOIN + GROUP BY + id ASC 보조 정렬로
   * 처리한다.
   */
  private List<Long> fetchCandidateIds(ExploreStandardCriteria criteria, int fetchSize) {
    SearchSortType sortType = criteria.sortType();
    com.querydsl.jpa.impl.JPAQuery<Long> query =
        queryFactory
            .select(alcohol.id)
            .from(alcohol)
            .join(region)
            .on(alcohol.region.id.eq(region.id))
            .join(distillery)
            .on(alcohol.distillery.id.eq(distillery.id));

    if (sortType != SearchSortType.RANDOM) {
      if (needsRatingJoin(sortType)) {
        query = query.leftJoin(rating).on(rating.id.alcoholId.eq(alcohol.id));
      }
      if (needsReviewJoin(sortType)) {
        query = query.leftJoin(review).on(review.alcoholId.eq(alcohol.id));
      }
      if (needsPicksJoin(sortType)) {
        query = query.leftJoin(picks).on(picks.alcoholId.eq(alcohol.id));
      }
    }

    query =
        query.where(
            supporter.keywordsMatch(criteria.keywords()),
            supporter.eqCategory(criteria.category()),
            supporter.inRegionIds(criteria.regionIds()),
            supporter.inDistilleryIds(criteria.distilleryIds()),
            supporter.eqCurationId(criteria.curationId()));

    if (sortType == SearchSortType.RANDOM) {
      return query
          .orderBy(supporter.sortByRandom())
          .offset(criteria.cursor())
          .limit(fetchSize)
          .fetch();
    }

    return query
        .groupBy(alcohol.id)
        .orderBy(supporter.sortBy(sortType, criteria.sortOrder()), alcohol.id.asc())
        .offset(criteria.cursor())
        .limit(fetchSize)
        .fetch();
  }

  private static boolean needsRatingJoin(SearchSortType sortType) {
    return sortType == SearchSortType.RATING || sortType == SearchSortType.POPULAR;
  }

  private static boolean needsReviewJoin(SearchSortType sortType) {
    return sortType == SearchSortType.REVIEW || sortType == SearchSortType.POPULAR;
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
