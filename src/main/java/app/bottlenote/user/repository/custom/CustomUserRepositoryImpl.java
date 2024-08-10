package app.bottlenote.user.repository.custom;

import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.user.dto.dsl.MyBottlePageableCriteria;
import app.bottlenote.user.dto.response.MyBottleResponse;
import app.bottlenote.user.dto.response.MyPageResponse;
import app.bottlenote.user.repository.UserQuerySupporter;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.picks.domain.QPicks.picks;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.domain.QReview.review;
import static app.bottlenote.user.domain.QUser.user;

@Slf4j
@RequiredArgsConstructor
public class CustomUserRepositoryImpl implements CustomUserRepository {

	private final JPAQueryFactory queryFactory;
	private final UserQuerySupporter supporter;

	/**
	 * 마이페이지 조회
	 *
	 * @param userId        마이페이지 조회 대상 사용자
	 * @param currentUserId 로그인 사용자
	 * @return MyPageResponse
	 */
	@Override
	public MyPageResponse getMyPage(Long userId, Long currentUserId) {

		return queryFactory
			.select(Projections.constructor(
				MyPageResponse.class,
				user.id.as("userId"),
				user.nickName.as("nickName"),
				user.imageUrl.as("userProfileImage"),
				supporter.reviewCountSubQuery(user.id),     // 마이 페이지 사용자의 리뷰 개수
				supporter.ratingCountSubQuery(user.id),     // 마이 페이지 사용자의 평점 개수
				supporter.picksCountSubQuery(user.id),      // 마이 페이지 사용자의 찜하기 개수
				supporter.followCountSubQuery(user.id),     // 마이 페이지 사용자가 팔로우 하는 유저 수
				supporter.followerCountSubQuery(user.id),   //  마이 페이지 사용자를 팔로우 하는 유저 수
				supporter.isFollowSubQuery(user.id, currentUserId), // 로그인 사용자가 마이 페이지 사용자를 팔로우 하고 있는지 여부
				supporter.isMyPageSubQuery(user.id, currentUserId) // 로그인 사용자가 마이 페이지 사용자인지 여부(나의 마이페이지인지 여부)
			))
			.from(user)
			.where(user.id.eq(userId))
			.fetchOne();

	}

	/**
	 * 마이 보틀 조회
	 *
	 * @param request MyBottlePageableCriteria
	 * @return MyBottleResponse
	 */
	@Override
	public MyBottleResponse getMyBottle(MyBottlePageableCriteria request) {

		Long userId = request.userId();

		boolean isMyPage = userId.equals(request.currentUserId());

		List<MyBottleResponse.MyBottleInfo> myBottleList = queryFactory
			.select(Projections.constructor(
				MyBottleResponse.MyBottleInfo.class,
				alcohol.id.as("alcoholId"),
				alcohol.korName.as("korName"),
				alcohol.engName.as("engName"),
				alcohol.korCategory.as("korCategoryName"),
				alcohol.imageUrl.as("imageUrl"),
				supporter.isPickedSubquery(alcohol.id, request.userId()),
				rating.ratingPoint.rating.coalesce(0.0).as("rating"),
				supporter.isMyReviewSubquery(alcohol.id, request.userId())
			))
			.from(alcohol)
			.leftJoin(user).on(user.id.eq(request.userId()))
			.leftJoin(review).on(review.userId.eq(user.id))
			.leftJoin(rating).on(rating.user.id.eq(user.id))
			.leftJoin(picks).on(picks.user.id.eq(user.id))
			.where(
				user.id.eq(request.userId()),
				request.regionId() != null ? supporter.eqRegion(request.regionId()) : null,
				request.tabType() != null ? supporter.eqTabType(request.tabType(), request.userId()) : null,
				request.keyword() != null && !request.keyword().isEmpty() ? supporter.eqName(request.keyword()) : null
			)
			.orderBy(supporter.sortBy(request.sortType(), request.sortOrder(), request.userId()))
			.offset(request.cursor())
			.limit(request.pageSize() + 1)
			.fetch();

		CursorPageable cursorPageable = supporter.myBottleCursorPageable(request, myBottleList);

		Long totalCount = (long) myBottleList.size();

		return MyBottleResponse.builder()
			.userId(userId)
			.isMyPage(isMyPage)
			.totalCount(totalCount)
			.myBottleList(myBottleList)
			.cursorPageable(cursorPageable)
			.build();
	}

}
