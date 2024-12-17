package app.bottlenote.rating.repository;

import app.bottlenote.alcohols.domain.constant.AlcoholCategoryGroup;
import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.rating.domain.constant.SearchSortType;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.util.StringUtils;
import com.querydsl.jpa.JPAExpressions;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.picks.domain.QPicks.picks;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.domain.QReview.review;

@Component
public class RatingQuerySupporter {

	protected BooleanExpression subQueryIsPicked(Long userId) {
		if (userId == null)
			return Expressions.FALSE;

		return Expressions.asBoolean(
			JPAExpressions
				.selectOne()
				.from(picks)
				.where(picks.alcohol.id.eq(alcohol.id),
					picks.user.id.eq(userId))
				.exists()
		);
	}

	/**
	 * CursorPageable 생성
	 *
	 * @param resultList the resultList
	 * @param cursor     the cursor
	 * @param pageSize   the page size
	 * @return the cursor pageable
	 */
	protected CursorPageable getCursorPageable(List<?> resultList, Long pageSize, Long cursor) {
		Objects.requireNonNull(resultList, "조회 결과 목록은 필수입니다.");
		Objects.requireNonNull(cursor, "커서는 필수입니다.");
		Objects.requireNonNull(pageSize, "조회 할 페이지 크기는 필수입니다.");

		int resultSize = resultList.size();
		boolean hasNext = resultSize > pageSize; // 다음 페이지가 있는지 확인
		if (hasNext) {
			resultList.remove(resultSize - 1);
		}

		return CursorPageable.builder()
			.currentCursor(cursor)
			.cursor(cursor + pageSize)  // 다음 페이지가 있는 경우 마지막으로 가져온 ID를 다음 커서로 사용
			.pageSize(pageSize)
			.hasNext(hasNext)
			.build();
	}

	/**
	 * 술 이름을 검색하는 조건
	 */
	protected BooleanExpression eqAlcoholName(String name) {

		if (StringUtils.isNullOrEmpty(name))
			return null;

		return alcohol.korName.like("%" + name + "%")
			.or(alcohol.engName.like("%" + name + "%"));
	}

	/**
	 * 카테고리를 검색하는 조건
	 */
	protected BooleanExpression eqAlcoholCategory(AlcoholCategoryGroup category) {

		if (Objects.isNull(category))
			return null;

		return alcohol.categoryGroup.stringValue().like("%" + category + "%");
	}

	/**
	 * 리전을 검색하는 조건
	 */
	protected BooleanExpression eqAlcoholRegion(Long regionId) {
		if (regionId == null)
			return null;

		return alcohol.region.id.eq(regionId);
	}

	/**
	 * 내가 별점을 안준 술만 조회
	 *
	 * @param userId the user id
	 * @return the boolean expression
	 */
	public BooleanExpression neRatingByMe(Long userId) {
		if (userId == null)
			return null;

		return rating.id.userId.isNull().or(rating.id.userId.ne(userId));
	}

	/**
	 * 1차 정렬 조건
	 * - RANDOM
	 * - POPULAR
	 * - RATING
	 * - PICK
	 * - REVIEW
	 */
	protected OrderSpecifier<?> orderBy(
		SearchSortType searchSortType,
		SortOrder sortOrder
	) {
		NumberExpression<Double> avgRating = rating.ratingPoint.rating.avg();  // 평균 평점 계산
		NumberExpression<Long> reviewCount = review.id.countDistinct();  // 고유 리뷰 수 계산
		NumberExpression<Long> pickCount = picks.id.countDistinct();  // 고유 좋아요 수 계산

		return switch (searchSortType) {
			case POPULAR ->
				sortOrder == SortOrder.DESC ? avgRating.add(reviewCount).desc() : avgRating.add(reviewCount).asc();
			case RATING -> sortOrder == SortOrder.DESC ? avgRating.desc() : avgRating.asc();
			case PICK -> sortOrder == SortOrder.DESC ? pickCount.desc() : pickCount.asc();
			case REVIEW -> sortOrder == SortOrder.DESC ? reviewCount.desc() : reviewCount.asc();
			case RANDOM -> Expressions.numberTemplate(Double.class, "function('rand')").asc();
		};
	}

	/**
	 * 2차 정렬 조건 (랜덤)
	 */
	protected OrderSpecifier<?> orderByRandom() {
		return Expressions.numberTemplate(Double.class, "function('rand')").asc();
	}

}
