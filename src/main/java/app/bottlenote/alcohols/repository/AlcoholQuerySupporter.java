package app.bottlenote.alcohols.repository;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.picks.domain.QPicks.picks;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.domain.QReview.review;

import app.bottlenote.alcohols.domain.constant.SearchSortType;
import app.bottlenote.alcohols.dto.dsl.AlcoholSearchCriteria;
import app.bottlenote.alcohols.dto.response.AlcoholInfo;
import app.bottlenote.alcohols.dto.response.AlcoholsSearchDetail;
import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.SortOrder;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.util.StringUtils;
import com.querydsl.jpa.JPAExpressions;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AlcoholQuerySupporter {

	/**
	 * 리뷰 상세 조회 시 사용되는 AlcoholInfo를 Projection하기 위한 메서드입니다.
	 *
	 * @param alcoholId 알코올 ID
	 * @param userId    유저 ID
	 * @return AlcoholInfo
	 */
	public ConstructorExpression<AlcoholInfo> alcoholInfoConstructor(Long alcoholId, Long userId) {
		return Projections.constructor(
			AlcoholInfo.class,
			alcohol.id.as("alcoholId"),
			alcohol.korName.as("korName"),
			alcohol.engName.as("engName"),
			alcohol.korCategory.as("korCategoryName"),
			alcohol.engCategory.as("engCategoryName"),
			alcohol.imageUrl.as("imageUrl"),
			isPickedSubquery(alcoholId, userId)
		);
	}

	public BooleanExpression isPickedSubquery(Long alcoholId, Long userId) {

		BooleanExpression eqUserId = userId == null ?
			picks.user.id.isNull() : picks.user.id.eq(userId);

		return Expressions.asBoolean(
			JPAExpressions
				.selectOne()
				.from(picks)
				.where(picks.alcohol.id.eq(alcoholId).and(eqUserId))
				.exists()
		).as("isPicked");
	}

	public NumberExpression<Double> isMyRatingSubquery(Long alcoholId, Long userId) {
		return Expressions.asNumber(
			JPAExpressions
				.select(rating.ratingPoint.rating.coalesce(0.0))
				.from(rating)
				.where(rating.alcohol.id.eq(alcoholId).and(rating.user.id.eq(userId)))
		).castToNum(Double.class).as("myRating");
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
	public CursorPageable getCursorPageable(AlcoholSearchCriteria criteriaDto, List<AlcoholsSearchDetail> fetch, Long cursor, Long pageSize) {
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
	public boolean isHasNext(AlcoholSearchCriteria criteriaDto, List<AlcoholsSearchDetail> fetch) {
		boolean hasNext = fetch.size() > criteriaDto.pageSize();

		if (hasNext) {
			fetch.remove(fetch.size() - 1);  // Remove the extra record
		}
		return hasNext;
	}

	/**
	 * 1차 정렬 조건 (인기순, 평점순, 좋아요순, 리뷰순)
	 */
	public OrderSpecifier<?> sortBy(SearchSortType searchSortType, SortOrder sortOrder) {
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
	public OrderSpecifier<?> sortByRandom() {
		return Expressions.numberTemplate(Double.class, "function('rand')").asc();
	}

	/**
	 * 토큰값이 유효한 경우 좋아요 상태를 나타내는 서브쿼리
	 */
	public BooleanExpression pickedSubQuery(Long userId) {
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
	public BooleanExpression eqName(String name) {

		if (StringUtils.isNullOrEmpty(name))
			return null;

		return alcohol.korName.like("%" + name + "%")
			.or(alcohol.engName.like("%" + name + "%"));
	}

	/**
	 * 카테고리를 검색하는 조건
	 */
	public BooleanExpression eqCategory(String category) {

		if (StringUtils.isNullOrEmpty(category))
			return null;

		return alcohol.korCategory.like("%" + category + "%")
			.or(alcohol.engCategory.like("%" + category + "%"));
	}

	/**
	 * 리전을 검색하는 조건
	 */
	public BooleanExpression eqRegion(Long regionId) {
		if (regionId == null)
			return null;

		return alcohol.region.id.eq(regionId);
	}

}
