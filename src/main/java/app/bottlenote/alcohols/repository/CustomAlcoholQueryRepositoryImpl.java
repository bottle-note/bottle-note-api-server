package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.constant.SearchSortType;
import app.bottlenote.alcohols.dto.dsl.AlcoholSearchCriteria;
import app.bottlenote.alcohols.dto.response.AlcoholSearchResponse;
import app.bottlenote.alcohols.dto.response.AlcoholsSearchDetail;
import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.global.service.cursor.SortOrder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.util.StringUtils;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.picks.domain.QPicks.picks;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.domain.QReview.review;


@RequiredArgsConstructor
public class CustomAlcoholQueryRepositoryImpl implements CustomAlcoholQueryRepository {
	private final JPAQueryFactory queryFactory;

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
				rating.id.count().as("ratingCount"),
				review.id.countDistinct().as("reviewCount"),
				picks.id.countDistinct().as("pickCount"),
				pickedSubQuery(userId).as("picked")
			))
			.from(alcohol)
			.leftJoin(rating).on(alcohol.id.eq(rating.alcohol.id))
			.leftJoin(picks).on(alcohol.id.eq(picks.alcohol.id))
			.leftJoin(review).on(alcohol.id.eq(review.alcohol.id))
			.where(
				eqName(criteriaDto.keyword()),
				eqCategory(criteriaDto.category()),
				eqRegion(criteriaDto.regionId())
			)

			.groupBy(alcohol.id, alcohol.korName, alcohol.engName, alcohol.korCategory, alcohol.engCategory, alcohol.imageUrl)
			.orderBy(sortBy(sortType, sortOrder))
			.orderBy(sortByRandom())
			.offset(cursor)
			.limit(criteriaDto.pageSize() + 1)  // 다음 페이지가 있는지 확인하기 위해 1개 더 가져옴
			.fetch();


		// where 조건으로 전체 결과값 카운트
		Long totalCount = queryFactory
			.select(alcohol.id.count())
			.from(alcohol)
			.where(
				eqName(criteriaDto.keyword()),
				eqCategory(criteriaDto.category()),
				eqRegion(criteriaDto.regionId())
			)
			.fetchOne();


		CursorPageable pageable = getCursorPageable(criteriaDto, fetch, cursor, pageSize);

		return PageResponse.of(AlcoholSearchResponse.of(totalCount, fetch), pageable);
	}

	/**
	 * CursorPageable 생성
	 *
	 * @param criteriaDto the criteria dto
	 * @param fetch       the fetch
	 * @param cursor      the cursor
	 * @param pageSize    the page size
	 * @return the cursor pageable
	 */
	private CursorPageable getCursorPageable(AlcoholSearchCriteria criteriaDto, List<AlcoholsSearchDetail> fetch, Long cursor, Long pageSize) {
		boolean hasNext = isHasNext(criteriaDto, fetch);
		return CursorPageable.builder()
			.currentCursor(cursor)
			.cursor(cursor + pageSize)  // 다음 페이지가 있는 경우 마지막으로 가져온 ID를 다음 커서로 사용
			.pageSize(pageSize)
			.hasNext(hasNext)
			.build();
	}

	/**
	 * 다음 페이지가 있는지 확인하는 메소드
	 */
	private boolean isHasNext(AlcoholSearchCriteria criteriaDto, List<AlcoholsSearchDetail> fetch) {
		boolean hasNext = fetch.size() > criteriaDto.pageSize();

		if (hasNext) {
			fetch.remove(fetch.size() - 1);  // Remove the extra record
		}
		return hasNext;
	}

	/**
	 * 1차 정렬 조건 (인기순, 평점순, 좋아요순, 리뷰순)
	 */
	private OrderSpecifier<?> sortBy(SearchSortType searchSortType, SortOrder sortOrder) {
		NumberExpression<Double> avgRating = rating.ratingPoint.rating.avg();  // 평균 평점 계산
		NumberExpression<Long> reviewCount = review.id.countDistinct();  // 고유 리뷰 수 계산
		NumberExpression<Long> pickCount = picks.id.countDistinct();  // 고유 좋아요 수 계산
		return switch (searchSortType) {
			case POPULAR ->
				sortOrder == SortOrder.DESC ? avgRating.add(reviewCount).desc() : avgRating.add(reviewCount).asc();
			case RATING -> sortOrder == SortOrder.DESC ? avgRating.desc() : avgRating.asc();
			case PICK -> sortOrder == SortOrder.DESC ? pickCount.desc() : pickCount.asc();
			case REVIEW -> sortOrder == SortOrder.DESC ? reviewCount.desc() : reviewCount.asc();
		};
	}

	/**
	 * 2차 정렬 조건 (랜덤)
	 */
	private OrderSpecifier<?> sortByRandom() {
		return Expressions.numberTemplate(Double.class, "function('rand')").asc();
	}

	/**
	 * 토큰값이 유효한 경우 좋아요 상태를 나타내는 서브쿼리
	 */
	private BooleanExpression pickedSubQuery(Long userId) {
		if (userId == null)
			return Expressions.asBoolean(false);

		return JPAExpressions
			.selectOne()
			.from(picks)
			.where(picks.alcohol.eq(alcohol),
				picks.user.id.eq(userId))
			.exists();
	}

	/**
	 * 술 이름을 검색하는 조건
	 */
	private BooleanExpression eqName(String name) {

		if (StringUtils.isNullOrEmpty(name))
			return null;

		return alcohol.korName.like("%" + name + "%")
			.or(alcohol.engName.like("%" + name + "%"));
	}

	/**
	 * 카테고리를 검색하는 조건
	 */
	private BooleanExpression eqCategory(String category) {

		if (StringUtils.isNullOrEmpty(category))
			return null;

		return alcohol.korCategory.like("%" + category + "%")
			.or(alcohol.engCategory.like("%" + category + "%"));
	}

	/**
	 * 리전을 검색하는 조건
	 */
	private BooleanExpression eqRegion(Long regionId) {
		if (regionId == null)
			return null;

		return alcohol.region.id.eq(regionId);
	}

}
