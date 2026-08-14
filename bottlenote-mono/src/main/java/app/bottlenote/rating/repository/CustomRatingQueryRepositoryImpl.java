package app.bottlenote.rating.repository;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.picks.domain.QPicks.picks;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.domain.QReview.review;

import app.bottlenote.global.pagination.HmacCursorCodec;
import app.bottlenote.global.pagination.PageResponse;
import app.bottlenote.global.pagination.Pagination;
import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.picks.repository.PicksQuerySupporter;
import app.bottlenote.rating.constant.SearchSortType;
import app.bottlenote.rating.dto.dsl.RatingListFetchCriteria;
import app.bottlenote.rating.dto.response.RatingListFetchResponse;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;

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
  public PageResponse<RatingListFetchResponse> fetchRatingList(RatingListFetchCriteria criteria) {
    Integer pageSize = criteria.size();
    Long userId = criteria.userId();
    SearchSortType sortType = criteria.sortType();
    SortOrder sortOrder = criteria.sortOrder();
    String context = ratingContext(criteria);
    Long lastId = null;
    if (criteria.cursor() != null) {
      lastId = Long.valueOf(cursorCodec.verify(criteria.cursor(), context).sortKeys().get("id"));
    }

    List<RatingListFetchResponse.Info> fetch =
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
            .groupBy(
                alcohol.id,
                alcohol.imageUrl,
                alcohol.korName,
                alcohol.engName,
                alcohol.korCategory,
                alcohol.engCategory)
            .orderBy(ratingQuerySupporter.orderBy(sortType, sortOrder), alcohol.id.asc())
            .having(lastId == null ? null : alcohol.id.gt(lastId))
            .limit(pageSize + 1L)
            .fetch();

    var slice =
        Pagination.fromOverflow(
            fetch,
            pageSize,
            item ->
                cursorCodec.encode(
                    context, java.util.Map.of("id", String.valueOf(item.alcoholId()))));
    return PageResponse.of(RatingListFetchResponse.create(slice.items()), slice.pagination());
  }

  private static String ratingContext(RatingListFetchCriteria criteria) {
    return "rating.list:"
        + criteria.userId()
        + ":"
        + criteria.keyword()
        + ":"
        + criteria.category()
        + ":"
        + criteria.regionId()
        + ":"
        + criteria.sortType()
        + ":"
        + criteria.sortOrder();
  }
}
