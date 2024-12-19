package app.bottlenote.user.repository;

import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.user.domain.constant.MyBottleSortType;
import app.bottlenote.user.domain.constant.MyBottleTabType;
import app.bottlenote.user.dto.dsl.MyBottlePageableCriteria;
import app.bottlenote.user.dto.response.MyBottleResponse;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.util.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.picks.domain.QPicks.picks;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.domain.QReview.review;
import static app.bottlenote.user.domain.QFollow.follow;
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
				.where(rating.id.userId.eq(userId)),
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
				.where(follow.userId.eq(userId)),
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
			.where(follow.userId.eq(currentUserId)
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
	public Predicate eqTabType(MyBottleTabType myBottleTabType) {
		return switch (myBottleTabType) {
			case PICK -> picks.id.isNotNull();
			case RATING -> rating.id.isNotNull();
			case REVIEW -> review.id.isNotNull();
			default -> null;
		};
	}

	/**
	 * 술 이름을 검색하는 조건
	 */
	public BooleanExpression eqName(String name) {
		if (StringUtils.isNullOrEmpty(name))
			return null;
		return alcohol.korName.like("%" + name + "%").or(alcohol.engName.like("%" + name + "%"));
	}


	/**
	 * 마이 보틀 정렬 조건을 반환
	 */
	public OrderSpecifier<?> sortBy(MyBottleSortType myBottleSortType, SortOrder sortOrder) {
		myBottleSortType = (myBottleSortType != null) ? myBottleSortType : MyBottleSortType.LATEST;
		sortOrder = (sortOrder != null) ? sortOrder : SortOrder.DESC; // 기본값은 내림차순

		return switch (myBottleSortType) {
			case RATING -> sortOrder.resolve(rating.ratingPoint.rating.max());
			case REVIEW -> sortOrder.resolve(review.createAt.max());
			default -> sortOrder.resolve(rating.lastModifyAt.coalesce(review.lastModifyAt, picks.lastModifyAt).max()
			);
		};
	}

}
