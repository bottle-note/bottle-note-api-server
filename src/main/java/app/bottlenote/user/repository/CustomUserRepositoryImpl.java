package app.bottlenote.user.repository;

import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.review.constant.ReviewActiveStatus;
import app.bottlenote.user.dto.dsl.MyBottlePageableCriteria;
import app.bottlenote.user.dto.response.MyBottleResponse;
import app.bottlenote.user.dto.response.MyPageResponse;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.picks.constant.PicksStatus.PICK;
import static app.bottlenote.picks.domain.QPicks.picks;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.domain.QReview.review;
import static app.bottlenote.user.constant.MyBottleTabType.ALL;
import static app.bottlenote.user.constant.MyBottleTabType.REVIEW;
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
				supporter.followingCountSubQuery(user.id),     // 마이 페이지 사용자가 팔로우 하는 유저 수
				supporter.followerCountSubQuery(user.id),   //  마이 페이지 사용자를 팔로우 하는 유저 수
				supporter.isFollowSubQuery(user.id, currentUserId), // 로그인 사용자가 마이 페이지 사용자를 팔로우 하고 있는지 여부
				supporter.isMyPageSubQuery(userId, currentUserId) // 로그인 사용자가 마이 페이지 사용자인지 여부(나의 마이페이지인지 여부)
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
				picks.id.countDistinct().gt(0).as("isPicked"),
				rating.ratingPoint.rating.coalesce(0.0).max().as("rating"),
				review.id.countDistinct().gt(0).as("hasReviewByMe"),
				rating.lastModifyAt.coalesce(review.lastModifyAt, picks.lastModifyAt).max().as("mostLastModifyAt"),
				rating.lastModifyAt.max().as("ratingLastModifyAt"),
				review.lastModifyAt.max().as("reviewLastModifyAt"),
				picks.lastModifyAt.max().as("picksLastModifyAt")
			))
			.from(alcohol)
			.leftJoin(picks).on(picks.alcoholId.eq(alcohol.id).and(picks.userId.eq(userId)).and(picks.status.eq(PICK)))
			.leftJoin(review).on(review.alcoholId.eq(alcohol.id).and(review.userId.eq(userId)).and(review.activeStatus.eq(ReviewActiveStatus.ACTIVE)))
			.leftJoin(rating).on(rating.id.alcoholId.eq(alcohol.id).and(rating.id.userId.eq(userId)).and(rating.ratingPoint.rating.gt(0.0)))
			.where(
				picks.id.isNotNull().or(rating.id.isNotNull()).or(review.id.isNotNull()),
				supporter.eqTabType(request.tabType()),
				supporter.eqName(request.keyword()),
				supporter.eqRegion(request.regionId())
			)
			.groupBy(alcohol.id, alcohol.korName, alcohol.engName, alcohol.korCategory, alcohol.imageUrl)
			.orderBy(supporter.sortBy(request.sortType(), request.sortOrder()))
			.offset(request.cursor())
			.limit(request.pageSize() + 1)
			.fetch();

		CursorPageable cursorPageable = supporter.myBottleCursorPageable(request, myBottleList);
		Long totalCount;
		// 공통 조건을 추출한 베이스 쿼리 생성
		JPAQuery<?> totalCountBaseQuery = queryFactory
			.from(alcohol)
			.leftJoin(picks).on(picks.alcoholId.eq(alcohol.id)
				.and(picks.userId.eq(userId))
				.and(picks.status.eq(PICK)))
			.leftJoin(review).on(review.alcoholId.eq(alcohol.id)
				.and(review.userId.eq(userId))
				.and(review.activeStatus.eq(ReviewActiveStatus.ACTIVE)))
			.leftJoin(rating).on(rating.id.alcoholId.eq(alcohol.id)
				.and(rating.id.userId.eq(userId))
				.and(rating.ratingPoint.rating.gt(0.0)))
			.where(
				picks.id.isNotNull().or(rating.id.isNotNull()).or(review.id.isNotNull()),
				supporter.eqTabType(request.tabType()),
				supporter.eqName(request.keyword()),
				supporter.eqRegion(request.regionId())
			);

		if (ALL.equals(request.tabType())) {
			// ALL인 경우는 각 카운트를 개별로 집계한 후 합산
			Tuple counts = totalCountBaseQuery.select(
				review.id.count().as("reviewCount"),
				rating.id.countDistinct().as("ratingCount"),
				picks.id.countDistinct().as("pickCount")
			).fetchOne();

			if (counts != null) {
				final Long reviewCountValue = counts.get(0, Long.class);
				final Long ratingCountValue = counts.get(1, Long.class);
				final Long pickCountValue = counts.get(2, Long.class);

				Long reviewCount = reviewCountValue != null ? reviewCountValue : 0L;
				Long ratingCount = ratingCountValue != null ? ratingCountValue : 0L;
				Long pickCount = pickCountValue != null ? pickCountValue : 0L;
				totalCount = reviewCount + ratingCount + pickCount;
				log.info("reviewCount : {}, ratingCount : {}, pickCount : {}", reviewCount, ratingCount, pickCount);
			} else {
				totalCount = 0L;
			}
		} else {
			totalCount = totalCountBaseQuery.select(
				request.tabType().equals(REVIEW) ? alcohol.id.count() : alcohol.id.countDistinct()
			).fetchOne();
		}
		return MyBottleResponse.builder()
			.userId(userId)
			.isMyPage(isMyPage)
			.totalCount(totalCount)
			.myBottleList(myBottleList)
			.cursorPageable(cursorPageable)
			.build();
	}
}
