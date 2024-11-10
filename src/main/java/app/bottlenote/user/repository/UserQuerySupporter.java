package app.bottlenote.user.repository;

import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.user.domain.constant.MyBottleSortType;
import app.bottlenote.user.domain.constant.MyBottleTabType;
import app.bottlenote.user.dto.dsl.MyBottlePageableCriteria;
import app.bottlenote.user.dto.response.MyBottleResponse;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.util.StringUtils;
import com.querydsl.jpa.JPAExpressions;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.follow.domain.QFollow.follow;
import static app.bottlenote.picks.domain.QPicks.picks;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.domain.QReview.review;
import static com.querydsl.jpa.JPAExpressions.select;

@Component
public class UserQuerySupporter {

	/**
	 * 마이 페이지 사용자의 리뷰 개수를 조회한다.
	 *
	 * @param userId
	 * @return 리뷰 개수
	 */
	public Expression<Long> reviewCountSubQuery(NumberPath<Long> userId) {
		return ExpressionUtils.as(
			select(review.count())
				.from(review)
				.where(review.userId.eq(userId)),
			"reviewCount"
		);
	}

	/**
	 * 마이 페이지 사용자의 평점 개수를 조회한다.
	 *
	 * @param userId
	 * @return 평점 개수
	 */
	public Expression<Long> ratingCountSubQuery(NumberPath<Long> userId) {
		return ExpressionUtils.as(
			select(rating.count())
				.from(rating)
				.where(rating.user.id.eq(userId)),
			"ratingCount"
		);
	}

	/**
	 * 마이 페이지 사용자의 찜하기 개수를 조회한다.
	 *
	 * @param userId
	 * @return 찜하기 개수
	 */
	public Expression<Long> picksCountSubQuery(NumberPath<Long> userId) {
		return ExpressionUtils.as(
			select(picks.count())
				.from(picks)
				.where(picks.user.id.eq(userId)),
			"picksCount"
		);
	}

	/**
	 * 마이 페이지 사용자가 팔로우 하는 유저 수 를 조회한다.
	 *
	 * @param userId
	 * @return 팔로우 하는 유저 수
	 */
	public Expression<Long> followCountSubQuery(NumberPath<Long> userId) {
		return ExpressionUtils.as(
			select(follow.count())
				.from(follow)
				.where(follow.user.id.eq(userId)),
			"followCount"
		);
	}

	/**
	 * 마이 페이지 사용자를 팔로우 하는 유저 수(팔로워 수)를 조회한다.
	 *
	 * @param userId
	 * @return 팔로워 수
	 */
	public Expression<Long> followerCountSubQuery(NumberPath<Long> userId) {
		return ExpressionUtils.as(
			select(follow.count())
				.from(follow)
				.where(follow.followUser.id.eq(userId)),
			"followerCount"
		);
	}

	/**
	 * 로그인 사용자가 마이 페이지 사용자를 팔로우 하고 있는지 상태 여부를 조회한다.
	 *
	 * @param userId
	 * @param currentUserId
	 * @return 팔로우 여부 (true : 팔로우 중, false : 팔로우 중이 아님)
	 */
	public BooleanExpression isFollowSubQuery(NumberPath<Long> userId, Long currentUserId) {
		return select(follow.count())
			.from(follow)
			.where(follow.user.id.eq(currentUserId)
				.and(follow.followUser.id.eq(userId)))
			.gt(0L);
	}

	/**
	 * 로그인 사용자가 조회하는 페이지의 사용자인지 여부(나의 마이페이지인지 여부)를 조회한다.
	 * 해당 조회는 마이보틀 페이지에서도 사용된다.
	 *
	 * @param userId
	 * @param currentUserId
	 * @return 마이페이지 여부 (true : 나의 마이페이지, false : 나의 마이페이지가 아님)
	 */
	public BooleanExpression isMyPageSubQuery(NumberPath<Long> userId, Long currentUserId) {
		return userId.eq(currentUserId);
	}

	/**
	 * 로그인 사용자가 해당 술에 대한 찜하기 상태를 조회한다.
	 *
	 * @param alcoholId
	 * @param userId
	 * @return 찜하기 상태 t/f
	 */
	public BooleanExpression isPickedSubquery(NumberPath<Long> alcoholId, Long userId) {
		return select(picks.count())
			.from(picks)
			.where(picks.alcohol.id.eq(alcoholId).and(picks.user.id.eq(userId)))
			.gt(0L);
	}

	/**
	 * 로그인 사용자가 해당 술에 대한 리뷰 작성 여부를 조회한다.
	 *
	 * @param alcoholId
	 * @param userId
	 * @return 리뷰 작성 여부 t/f
	 */
	public BooleanExpression isMyReviewSubquery(NumberPath<Long> alcoholId, Long userId) {
		return select(review.count())
			.from(review)
			.where(review.alcoholId.eq(alcoholId).and(review.userId.eq(userId)))
			.gt(0L);
	}

	/**
	 * 마이 보틀 CursorPageable 생성
	 *
	 * @param request      MyBottlePageableCriteria
	 * @param myBottleList List<MyBottleResponse.MyBottleInfo>
	 * @return CursorPageable
	 */
	public CursorPageable myBottleCursorPageable(MyBottlePageableCriteria request, List<MyBottleResponse.MyBottleInfo> myBottleList) {
		boolean hasNext = isHasNext(request, myBottleList);
		return CursorPageable.builder()
			.cursor(request.cursor() + request.pageSize())
			.pageSize(request.pageSize())
			.hasNext(hasNext)
			.currentCursor(request.cursor())
			.build();
	}

	private boolean isHasNext(MyBottlePageableCriteria request, List<MyBottleResponse.MyBottleInfo> myBottleList) {
		boolean hasNext = myBottleList.size() > request.pageSize();
		if (hasNext) {
			myBottleList.remove(myBottleList.size() - 1);
		}
		return hasNext;
	}

	/**
	 * 지역(리전) 검색조건
	 */
	public BooleanExpression eqRegion(Long regionId) {
		if (regionId == null)
			return null;

		return alcohol.region.id.eq(regionId);
	}

	/**
	 * 탭 타입 검색조건
	 */
	public Predicate eqTabType(MyBottleTabType myBottleTabType, Long userId) {

		if (myBottleTabType == null) {
			return null;
		}

		// 타입별로 검색 조건을 미리 정의
		BooleanExpression pickCondition = JPAExpressions
			.selectOne()
			.from(picks)
			.where(picks.user.id.eq(userId).and(picks.alcohol.id.eq(alcohol.id)))
			.exists();

		BooleanExpression ratingCondition = JPAExpressions
			.selectOne()
			.from(rating)
			.where(rating.user.id.eq(userId)
				.and(rating.alcohol.id.eq(alcohol.id)
					.and(rating.ratingPoint.rating.gt(0.0))))
			.exists();

		BooleanExpression reviewCondition = JPAExpressions
			.selectOne()
			.from(review)
			.where(review.userId.eq(userId).and(review.alcoholId.eq(alcohol.id)))
			.exists();

		if (myBottleTabType == null || myBottleTabType == MyBottleTabType.ALL) {
			// 모든 타입에서 활동이 있었는지 검사 (찜하기, 별점, 리뷰)
			return pickCondition.or(ratingCondition).or(reviewCondition);
		} else {
			// 지정된 타입에 따라 검사
			switch (myBottleTabType) {
				case PICK:
					return pickCondition;
				case RATING:
					return ratingCondition;
				case REVIEW:
					return reviewCondition;
				default:
					return null; // 없는 경우는 null 반환
			}
		}
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
	 * 마이 보틀 정렬 조건
	 */
	public OrderSpecifier<?> sortBy(MyBottleSortType myBottleSortType, SortOrder sortOrder) {

		myBottleSortType = (myBottleSortType != null) ? myBottleSortType : MyBottleSortType.LATEST;

		// userId 조건을 제거한 Expression 생성
		Expression<Double> myRating = JPAExpressions
			.select(rating.ratingPoint.rating.coalesce(0.0))
			.from(rating);

		Expression<Long> reviewCount = JPAExpressions
			.select(review.count())
			.from(review);

		Expression<LocalDateTime> latestUpdate = JPAExpressions
			.select(rating.lastModifyAt.max().coalesce(review.lastModifyAt.max()
				.coalesce(picks.lastModifyAt.max())))
			.from(rating, review, picks);

		// 새로운 switch 표현식을 사용하여 정렬
		return switch (myBottleSortType) {
			case RATING -> new OrderSpecifier<>(sortOrder == SortOrder.DESC ? Order.DESC : Order.ASC, myRating);
			case REVIEW -> new OrderSpecifier<>(sortOrder == SortOrder.DESC ? Order.DESC : Order.ASC, reviewCount);
			case LATEST -> new OrderSpecifier<>(sortOrder == SortOrder.DESC ? Order.DESC : Order.ASC, latestUpdate);
		};
	}

}
