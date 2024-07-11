package app.bottlenote.alcohols.repository.custom;

import app.bottlenote.alcohols.domain.constant.SearchSortType;
import app.bottlenote.alcohols.dto.dsl.AlcoholSearchCriteria;
import app.bottlenote.alcohols.dto.response.AlcoholInfo;
import app.bottlenote.alcohols.dto.response.AlcoholSearchResponse;
import app.bottlenote.alcohols.dto.response.AlcoholsSearchDetail;
import app.bottlenote.alcohols.dto.response.detail.AlcoholDetailInfo;
import app.bottlenote.alcohols.repository.AlcoholQuerySupporter;
import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.global.service.cursor.SortOrder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.alcohols.domain.QDistillery.distillery;
import static app.bottlenote.alcohols.domain.QRegion.region;
import static app.bottlenote.picks.domain.QPicks.picks;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.domain.QReview.review;


public class CustomAlcoholQueryRepositoryImpl implements CustomAlcoholQueryRepository {
	private final JPAQueryFactory queryFactory;
	private final AlcoholQuerySupporter supporter;

	public CustomAlcoholQueryRepositoryImpl(JPAQueryFactory queryFactory, AlcoholQuerySupporter supporter) {
		this.queryFactory = queryFactory;
		this.supporter = supporter;
	}

	/**
	 * queryDSL을 이용한 알코올 상세 조회
	 *
	 * @param alcoholId 조회 대상 알코올 ID
	 * @param userId    만약 사용자가 로그인한 경우 좋아요 상태를 확인하기 위한 사용자 ID
	 * @return the alcohol detail info
	 */
	@Override

	public AlcoholDetailInfo findAlcoholDetailById(Long alcoholId, Long userId) {

		if (Objects.isNull(userId)) userId = -1L;

		List<String> tags = List.of("달달한", "부드러운", "향긋한", "견과류", "후추향의");

		return queryFactory
			.select(Projections.constructor(
				AlcoholDetailInfo.class,
				alcohol.id.as("alcoholId"),
				alcohol.imageUrl.as("alcoholUrlImg"),
				alcohol.korName.as("korName"),
				alcohol.engName.as("engName"),
				alcohol.korCategory.as("korCategory"),
				alcohol.engCategory.as("engCategory"),
				region.korName.as("korRegion"),
				region.engName.as("engRegion"),
				alcohol.cask.as("cask"),
				alcohol.abv.as("avg"),
				distillery.korName.as("korDistillery"),
				distillery.engName.as("engDistillery"),
				rating.ratingPoint.rating.avg().multiply(2).castToNum(Double.class).round().divide(2).coalesce(0.0).as("rating"),
				rating.id.count().as("totalRatingsCount"),
				supporter.isMyRatingSubquery(alcoholId, userId),
				supporter.isPickedSubquery(alcoholId, userId),
				Expressions.constant(tags) // 여기서 tags 리스트를 상수로 전달
			))
			.from(alcohol)
			.leftJoin(rating).on(alcohol.id.eq(rating.alcohol.id))
			.join(region).on(alcohol.region.id.eq(region.id))
			.join(distillery).on(alcohol.distillery.id.eq(distillery.id))
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
				distillery.engName
			)
			.fetchOne();
	}

	/**
	 * 리뷰 상세 조회 시 포함 될 술의 정보를 조회합니다.
	 *
	 * @param alcoholId 조회 대상 AlcoholId
	 * @param userId    만약 사용자가 로그인한 경우 좋아요 상태를 확인하기 위한 사용자 ID
	 * @return AlcoholInfo
	 */
	@Override
	public Optional<AlcoholInfo> findAlcoholInfoById(Long alcoholId, Long userId) {

		return Optional.ofNullable(queryFactory
			.select(supporter.alcoholInfoConstructor(alcoholId, userId))
			.from(alcohol)
			.leftJoin(rating).on(alcohol.id.eq(rating.alcohol.id))
			.join(region).on(alcohol.region.id.eq(region.id))
			.join(distillery).on(alcohol.distillery.id.eq(distillery.id))
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
				distillery.engName
			)
			.fetchOne());
	}

	/**
	 * queryDSL을 이용한 알코올 검색
	 * <p>
	 * 구현중 2024/05/03
	 *
	 * @param criteriaDto the criteria dto
	 * @return the slice
	 */
	@Override
	public PageResponse<AlcoholSearchResponse> searchAlcohols(AlcoholSearchCriteria criteriaDto) {
		Long cursor = criteriaDto.cursor();
		Long pageSize = criteriaDto.pageSize();
		SearchSortType sortType = criteriaDto.sortType();
		SortOrder sortOrder = criteriaDto.sortOrder();

		Long userId = criteriaDto.userId();

		List<AlcoholsSearchDetail> fetch = queryFactory
			.select(Projections.fields(
				AlcoholsSearchDetail.class,
				alcohol.id.as("alcoholId"),
				alcohol.korName.as("korName"),
				alcohol.engName.as("engName"),
				alcohol.korCategory.as("korCategoryName"),
				alcohol.engCategory.as("engCategoryName"),
				alcohol.imageUrl.as("imageUrl"),
				rating.ratingPoint.rating.avg().multiply(2).castToNum(Double.class).round().divide(2).coalesce(0.0).as("rating"),
				rating.id.alcoholId.countDistinct().as("ratingCount"),
				review.id.countDistinct().as("reviewCount"),
				picks.id.countDistinct().as("pickCount"),
				supporter.pickedSubQuery(userId).as("isPicked")
			))
			.from(alcohol)
			.leftJoin(rating).on(alcohol.id.eq(rating.alcohol.id))
			.leftJoin(picks).on(alcohol.id.eq(picks.alcohol.id))
			.leftJoin(review).on(alcohol.id.eq(review.alcoholId))
			.where(
				supporter.eqName(criteriaDto.keyword()),
				supporter.eqCategory(criteriaDto.category()),
				supporter.eqRegion(criteriaDto.regionId())
			)
			.groupBy(alcohol.id, alcohol.korName, alcohol.engName, alcohol.korCategory, alcohol.engCategory, alcohol.imageUrl)
			.orderBy(supporter.sortBy(sortType, sortOrder))
			.orderBy(supporter.sortByRandom())
			.offset(cursor)
			.limit(criteriaDto.pageSize() + 1)  // 다음 페이지가 있는지 확인하기 위해 1개 더 가져옴
			.fetch();


		// where 조건으로 전체 결과값 카운트
		Long totalCount = queryFactory
			.select(alcohol.id.count())
			.from(alcohol)
			.where(
				supporter.eqName(criteriaDto.keyword()),
				supporter.eqCategory(criteriaDto.category()),
				supporter.eqRegion(criteriaDto.regionId())
			)
			.fetchOne();


		CursorPageable pageable = supporter.getCursorPageable(criteriaDto, fetch, cursor, pageSize);
		return PageResponse.of(AlcoholSearchResponse.of(totalCount, fetch), pageable);
	}


}
