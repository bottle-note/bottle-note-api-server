package app.bottlenote.rating.repository;

import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.rating.domain.constant.SearchSortType;
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

	private final RatingQuerySupporter querySupporter;
	private final JPAQueryFactory queryFactory;

	public CustomRatingQueryRepositoryImpl(RatingQuerySupporter ratingQuerySupporter, JPAQueryFactory queryFactory) {
		this.querySupporter = ratingQuerySupporter;
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
					querySupporter.subQueryIsPicked(userId)
				)
			).from(alcohol)
			.leftJoin(rating).on(alcohol.id.eq(rating.alcohol.id))
			.leftJoin(picks).on(alcohol.id.eq(picks.alcohol.id))
			.leftJoin(review).on(alcohol.id.eq(review.alcohol.id))
			.where(
				querySupporter.eqAlcoholName(criteria.keyword()),
				querySupporter.eqAlcoholCategory(criteria.category()),
				querySupporter.eqAlcoholRegion(criteria.regionId())
			)
			.groupBy(
				alcohol.id,
				alcohol.imageUrl,
				alcohol.korName,
				alcohol.engName,
				alcohol.korCategory,
				alcohol.engCategory)
			.orderBy(querySupporter.orderBy(sortType, sortOrder))
			.orderBy(querySupporter.orderByRandom())
			.offset(cursor)
			.limit(pageSize + 1)
			.fetch();

		Long totalCount = queryFactory
			.select(alcohol.id.count())
			.from(alcohol)
			.where(
				querySupporter.eqAlcoholName(criteria.keyword()),
				querySupporter.eqAlcoholCategory(criteria.category()),
				querySupporter.eqAlcoholRegion(criteria.regionId())
			)
			.fetchOne();

		var pageable = querySupporter.getCursorPageable(fetch, pageSize, cursor);
		var fetchResponse = RatingListFetchResponse.create(totalCount, fetch);

		return PageResponse.of(fetchResponse, pageable);
	}

}
