package app.bottlenote.alcohols.repository;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.alcohols.domain.QDistillery.distillery;
import static app.bottlenote.alcohols.domain.QRegion.region;
import static app.bottlenote.alcohols.repository.AlcoholQuerySupporter.getTastingTags;
import static app.bottlenote.picks.domain.QPicks.picks;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.domain.QReview.review;

import app.bottlenote.alcohols.dto.dsl.AlcoholSearchCriteria;
import app.bottlenote.alcohols.dto.response.AlcoholDetailItem;
import app.bottlenote.alcohols.dto.response.AlcoholSearchResponse;
import app.bottlenote.alcohols.dto.response.AlcoholsSearchItem;
import app.bottlenote.alcohols.facade.payload.AlcoholSummaryItem;
import app.bottlenote.shared.alcohols.constant.SearchSortType;
import app.bottlenote.shared.cursor.CursorPageable;
import app.bottlenote.shared.cursor.CursorResponse;
import app.bottlenote.shared.cursor.PageResponse;
import app.bottlenote.shared.cursor.SortOrder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;

@AllArgsConstructor
public class CustomAlcoholQueryRepositoryImpl implements CustomAlcoholQueryRepository {
  private final JPAQueryFactory queryFactory;
  private final AlcoholQuerySupporter supporter;

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
            .orderBy(supporter.sortByRandom())
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
                supporter.eqCategory(criteriaDto.category()),
                supporter.eqRegion(criteriaDto.regionId()))
            .fetchOne();

    CursorPageable pageable = CursorPageable.of(fetch, cursor, pageSize);
    return PageResponse.of(AlcoholSearchResponse.of(totalCount, fetch), pageable);
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
                rating.id.count(),
                supporter.myRating(alcoholId, userId),
                supporter.averageReviewRating(alcoholId, userId),
                supporter.isPickedSubquery(alcoholId, userId),
                getTastingTags()))
        .from(alcohol)
        .leftJoin(rating)
        .on(rating.id.alcoholId.eq(alcohol.id))
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
  public Pair<Long, CursorResponse<AlcoholDetailItem>> getStandardExplore(
      Long userId, List<String> keyword, Long cursor, Integer pageSize) {
    int fetchSize = pageSize + 1;
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
                    rating.id.count(),
                    supporter.myRating(alcohol.id, userId),
                    supporter.averageReviewRating(alcohol.id, userId),
                    supporter.isPickedSubquery(alcohol.id, userId),
                    getTastingTags()))
            .from(alcohol)
            .leftJoin(rating)
            .on(rating.id.alcoholId.eq(alcohol.id))
            .join(region)
            .on(alcohol.region.id.eq(region.id))
            .join(distillery)
            .on(alcohol.distillery.id.eq(distillery.id))
            .where(supporter.keywordsMatch(keyword))
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
            .orderBy(alcohol.lastModifyAt.desc())
            .offset(cursor)
            .limit(fetchSize)
            .fetch();

    Long total =
        queryFactory
            .select(alcohol.id.count())
            .from(alcohol)
            .join(region)
            .on(alcohol.region.id.eq(region.id))
            .join(distillery)
            .on(alcohol.distillery.id.eq(distillery.id))
            .where(supporter.keywordsMatch(keyword))
            .fetchOne();
    CursorResponse<AlcoholDetailItem> list = CursorResponse.of(items, cursor, pageSize);
    return Pair.of(total, list);
  }
}
