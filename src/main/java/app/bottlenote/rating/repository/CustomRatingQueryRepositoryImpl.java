package app.bottlenote.rating.repository;

import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.picks.repository.PicksQuerySupporter;
import app.bottlenote.rating.constant.SearchSortType;
import app.bottlenote.rating.dto.dsl.RatingListFetchCriteria;
import app.bottlenote.rating.dto.response.RatingListFetchResponse;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;

import java.util.List;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.picks.domain.QPicks.picks;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.domain.QReview.review;

public class CustomRatingQueryRepositoryImpl implements CustomRatingQueryRepository {

	private final RatingQuerySupporter ratingQuerySupporter;
	private final PicksQuerySupporter picksQuerySupporter;
	private final JPAQueryFactory queryFactory;

	public CustomRatingQueryRepositoryImpl(RatingQuerySupporter ratingQuerySupporter, PicksQuerySupporter picksQuerySupporter, JPAQueryFactory queryFactory) {
		this.ratingQuerySupporter = ratingQuerySupporter;
		this.picksQuerySupporter = picksQuerySupporter;
		this.queryFactory = queryFactory;
	}

	@Override
	public PageResponse<RatingListFetchResponse> fetchRatingList(RatingListFetchCriteria criteria) {
		Long cursor = criteria.cursor();
		Long pageSize = criteria.pageSize();
		Long userId = criteria.userId();
		SearchSortType sortType = criteria.sortType();
		SortOrder sortOrder = criteria.sortOrder();

		List<RatingListFetchResponse.Info> fetch = queryFactory.select(
						Projections.constructor(
								RatingListFetchResponse.Info.class,
								alcohol.id.as("alcoholId"),
								alcohol.imageUrl.as("imageUrl"),
								alcohol.korName.as("korName"),
								alcohol.engName.as("engName"),
								alcohol.korCategory.as("korCategoryName"),
								alcohol.engCategory.as("engCategoryName"),
								picksQuerySupporter.isPickedSubQuery(userId)
						)
				).from(alcohol)
				.leftJoin(rating).on(alcohol.id.eq(rating.id.alcoholId))
				.leftJoin(picks).on(alcohol.id.eq(picks.alcoholId))
				.leftJoin(review).on(alcohol.id.eq(review.alcoholId))
				.where(
						ratingQuerySupporter.eqAlcoholName(criteria.keyword()),
						ratingQuerySupporter.eqAlcoholCategory(criteria.category()),
						ratingQuerySupporter.eqAlcoholRegion(criteria.regionId()),
						ratingQuerySupporter.neRatingByMe(userId)
				)
				.groupBy(
						alcohol.id,
						alcohol.imageUrl,
						alcohol.korName,
						alcohol.engName,
						alcohol.korCategory,
						alcohol.engCategory)
				.orderBy(ratingQuerySupporter.orderBy(sortType, sortOrder))
				.orderBy(ratingQuerySupporter.orderByRandom())
				.offset(cursor)
				.limit(pageSize + 1)
				.fetch();

		Long totalCount = queryFactory
				.select(alcohol.id.count())
				.from(alcohol)
				.where(
						ratingQuerySupporter.eqAlcoholName(criteria.keyword()),
						ratingQuerySupporter.eqAlcoholCategory(criteria.category()),
						ratingQuerySupporter.eqAlcoholRegion(criteria.regionId())
				)
				.fetchOne();

		var pageable = ratingQuerySupporter.getCursorPageable(fetch, pageSize, cursor);
		var fetchResponse = RatingListFetchResponse.create(totalCount, fetch);

		return PageResponse.of(fetchResponse, pageable);
	}

}
