package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.constant.AlcoholCategoryGroup;
import app.bottlenote.alcohols.domain.constant.SearchSortType;
import app.bottlenote.alcohols.dto.dsl.AlcoholSearchCriteria;
import app.bottlenote.alcohols.dto.response.AlcoholsSearchDetail;
import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.SortOrder;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
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
import static app.bottlenote.alcohols.domain.QAlcoholsTastingTags.alcoholsTastingTags;
import static app.bottlenote.alcohols.domain.QTastingTag.tastingTag;
import static app.bottlenote.picks.domain.PicksStatus.PICK;
import static app.bottlenote.picks.domain.QPicks.picks;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.domain.QReview.review;

@Component
public class AlcoholQuerySupporter {

	public static Expression<String> getTastingTags() {
		return ExpressionUtils.as(
			JPAExpressions.select(
					Expressions.stringTemplate("group_concat({0})", tastingTag.korName)
				)
				.from(alcoholsTastingTags)
				.join(tastingTag).on(alcoholsTastingTags.tastingTag.id.eq(tastingTag.id))
				.where(alcoholsTastingTags.alcohol.id.eq(alcohol.id)),
			"alcoholsTastingTags"
		);
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
			.where(picks.alcoholId.eq(alcohol.id),
				picks.userId.eq(userId))
			.exists();
	}

	public BooleanExpression isPickedSubquery(Long alcoholId, Long userId) {
		if (userId == -1) {
			return Expressions.asBoolean(false);
		}
		return Expressions.asBoolean(
			JPAExpressions
				.selectOne()
				.from(picks)
				.where(picks.alcoholId.eq(alcoholId).and(picks.userId.eq(userId)).and(picks.status.eq(PICK)))
				.exists()
		).as("isPicked");
	}

	public NumberExpression<Double> myRating(Long alcoholId, Long userId) {
		return Expressions.asNumber(
			JPAExpressions
				.select(rating.ratingPoint.rating)
				.from(rating)
				.where(rating.id.alcoholId.eq(alcoholId)
					.and(rating.id.userId.eq(userId)))
				.limit(1)
		).coalesce(0.0).castToNum(Double.class).as("myRating");
	}

	public NumberExpression<Double> averageReviewRating(Long alcoholId, Long userId) {
		//	0.25 단위로 평균 별점을 조회
		return Expressions.asNumber(
			JPAExpressions
				.select(
					review.reviewRating.avg().multiply(2).castToNum(Double.class).round().divide(2).coalesce(0.0)
				)
				.from(review)
				.where(review.alcoholId.eq(alcoholId)
					.and(review.userId.eq(userId)))
		).castToNum(Double.class).as("myAvgRating");
	}

	public CursorPageable getCursorPageable(AlcoholSearchCriteria criteriaDto, List<AlcoholsSearchDetail> fetch, Long cursor, Long pageSize) {
		boolean hasNext = isHasNext(criteriaDto, fetch);
		return CursorPageable.builder()
			.currentCursor(cursor)
			.cursor(cursor + pageSize)  // 다음 페이지가 있는 경우 마지막으로 가져온 ID를 다음 커서로 사용
			.pageSize(pageSize)
			.hasNext(hasNext)
			.build();
	}

	public boolean isHasNext(AlcoholSearchCriteria criteriaDto, List<AlcoholsSearchDetail> fetch) {
		boolean hasNext = fetch.size() > criteriaDto.pageSize();

		if (hasNext) {
			fetch.remove(fetch.size() - 1);  // Remove the extra record
		}
		return hasNext;
	}

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

	public OrderSpecifier<?> sortByRandom() {
		return Expressions.numberTemplate(Double.class, "function('rand')").asc();
	}

	public BooleanExpression eqName(String name) {

		if (StringUtils.isNullOrEmpty(name))
			return null;

		return alcohol.korName.like("%" + name + "%")
			.or(alcohol.engName.like("%" + name + "%"));
	}

	public BooleanExpression eqCategory(AlcoholCategoryGroup category) {
		if (Objects.isNull(category))
			return null;

		return alcohol.categoryGroup.stringValue().like("%" + category + "%");
	}

	public BooleanExpression eqRegion(Long regionId) {
		if (regionId == null)
			return null;

		return alcohol.region.id.eq(regionId);
	}
}
