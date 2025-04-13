package app.bottlenote.user.repository;

import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.picks.constant.PicksStatus;
import app.bottlenote.review.constant.ReviewActiveStatus;
import app.bottlenote.user.constant.FollowStatus;
import app.bottlenote.user.constant.MyBottleSortType;
import app.bottlenote.user.constant.MyBottleType;
import app.bottlenote.user.dto.dsl.MyBottlePageableCriteria;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.util.StringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.alcohols.domain.QPopularAlcohol.popularAlcohol;
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
	 * @param userId 마이 페이지 사용자
	 * @return 리뷰 개수
	 */
	public Expression<Long> reviewCountSubQuery(NumberPath<Long> userId) {
		return ExpressionUtils.as(
				select(review.count())
						.from(review)
						.where(review.userId.eq(userId)
								.and(review.activeStatus.eq(ReviewActiveStatus.ACTIVE))),
				"reviewCount"
		);
	}

	/**
	 * 마이 페이지 사용자의 평점 개수를 조회한다.
	 *
	 * @param userId 마이 페이지 사용자
	 * @return 평점 개수
	 */
	public Expression<Long> ratingCountSubQuery(NumberPath<Long> userId) {
		return ExpressionUtils.as(
				select(rating.count())
						.from(rating)
						.where(rating.id.userId.eq(userId)
								.and(rating.ratingPoint.rating.gt(0.0))),
				"ratingCount"
		);
	}

	/**
	 * 마이 페이지 사용자의 찜하기 개수를 조회한다.
	 *
	 * @param userId 마이 페이지 사용자
	 * @return 찜하기 개수
	 */
	public Expression<Long> picksCountSubQuery(NumberPath<Long> userId) {
		return ExpressionUtils.as(
				select(picks.count())
						.from(picks)
						.where(picks.userId.eq(userId)
								.and(picks.status.eq(PicksStatus.PICK))),
				"picksCount"
		);
	}

	/**
	 * 마이 페이지 사용자의 팔로워 수 를 조회한다.
	 *
	 * @param userId 마이 페이지 사용자
	 * @return 팔로워 수
	 */
	public Expression<Long> followerCountSubQuery(NumberPath<Long> userId) {
		return ExpressionUtils.as(
				select(follow.count())
						.from(follow)
						.where(follow.userId.eq(userId)
								.and(follow.status.eq(FollowStatus.FOLLOWING))),
				"followCount"
		);
	}

	/**
	 * 마이 페이지 사용자가 팔로잉 하는 유저 수를 조회한다.
	 *
	 * @param userId 마이 페이지 사용자
	 * @return 팔로잉 수
	 */
	public Expression<Long> followingCountSubQuery(NumberPath<Long> userId) {
		return ExpressionUtils.as(
				select(follow.count())
						.from(follow)
						.where(follow.targetUserId.eq(userId)
								.and(follow.status.eq(FollowStatus.FOLLOWING))),
				"followerCount"
		);
	}

	/**
	 * 로그인 사용자가 마이 페이지 사용자를 팔로우 하고 있는지 상태 여부를 조회한다.
	 *
	 * @param userId        마이 페이지 사용자
	 * @param currentUserId 로그인 사용자
	 * @return 팔로우 여부 (true : 팔로우 중, false : 팔로우 중이 아님)
	 */
	public BooleanExpression isFollowSubQuery(NumberPath<Long> userId, Long currentUserId) {
		return select(follow.count())
				.from(follow)
				.where(follow.userId.eq(currentUserId)
						.and(follow.targetUserId.eq(userId))
						.and(follow.status.eq(FollowStatus.FOLLOWING)))
				.gt(0L);
	}

	/**
	 * 로그인 사용자가 조회하는 페이지의 사용자인지 여부(나의 마이페이지인지 여부)를 조회한다. 해당 조회는 마이보틀 페이지에서도 사용된다.
	 *
	 * @param userId        조회하는 페이지의 사용자
	 * @param currentUserId 로그인 사용자
	 * @return 마이페이지 여부 (true : 나의 마이페이지, false : 나의 마이페이지가 아님)
	 */
	public BooleanExpression isMyPageSubQuery(Long userId, Long currentUserId) {
		return Expressions.asBoolean(Objects.equals(userId, currentUserId));
	}

	/**
	 * 로그인 사용자가 해당 술에 대한 리뷰 작성 여부를 조회한다.
	 *
	 * @param alcoholId 술 ID
	 * @param userId    로그인 사용자 ID
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
	public CursorPageable myBottleCursorPageable(MyBottlePageableCriteria request, List<?> myBottleList) {
		boolean hasNext = isHasNext(request, myBottleList);
		return CursorPageable.builder()
				.cursor(request.cursor() + request.pageSize())
				.pageSize(request.pageSize())
				.hasNext(hasNext)
				.currentCursor(request.cursor())
				.build();
	}

	private boolean isHasNext(MyBottlePageableCriteria request, List<?> myBottleList) {
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
	public OrderSpecifier<?> sortBy(MyBottleType tabType, MyBottleSortType myBottleSortType, SortOrder sortOrder) {
		myBottleSortType = (myBottleSortType != null) ? myBottleSortType : MyBottleSortType.LATEST;
		sortOrder = (sortOrder != null) ? sortOrder : SortOrder.DESC; // 기본값은 내림차순

		return switch (myBottleSortType) {
			case RATING -> sortOrder.resolve(rating.ratingPoint.rating.max());
			case REVIEW -> sortOrder.resolve(review.createAt.max());
			case LATEST -> switch (tabType) {
				case PICK -> sortOrder.resolve(picks.lastModifyAt);
				case REVIEW -> sortOrder.resolve(review.lastModifyAt);
				case RATING -> sortOrder.resolve(rating.lastModifyAt);
			};
		};
	}

	/**
	 * 파라미터로 받은 alcoholId가 popularAlcohol에 속하는지 여부
	 *
	 * @param id
	 * @return
	 */
	public BooleanExpression isHot5(NumberPath<Long> id) {
		LocalDateTime now = LocalDateTime.now();
		return select(popularAlcohol.count())
				.from(popularAlcohol)
				.where(popularAlcohol.alcoholId.eq(id),
						popularAlcohol.year.eq(now.getYear()),
						popularAlcohol.month.eq(now.getMonthValue()),
						popularAlcohol.day.eq(now.getDayOfMonth())
				)
				.gt(0L);
	}
}
