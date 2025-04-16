package app.bottlenote.user.repository;

import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.review.constant.ReviewActiveStatus;
import app.bottlenote.review.domain.QReviewTastingTag;
import app.bottlenote.user.constant.MyBottleType;
import app.bottlenote.user.dto.dsl.MyBottlePageableCriteria;
import app.bottlenote.user.dto.response.MyBottleResponse;
import app.bottlenote.user.dto.response.MyPageResponse;
import app.bottlenote.user.dto.response.RatingMyBottleItem;
import app.bottlenote.user.dto.response.ReviewMyBottleItem;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
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
						supporter.ratingCountSubQuery(userId),     // 마이 페이지 사용자의 평점 개수
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
//	@Override
//	public MyBottleResponse getMyBottle(MyBottlePageableCriteria request) {
//		Long userId = request.userId();
//		boolean isMyPage = userId.equals(request.currentUserId());
//
//		List<MyBottleResponse.BaseMyBottleInfo> myBottleList = queryFactory
//				.select(Projections.constructor(
//						MyBottleResponse.BaseMyBottleInfo.class,
//						alcohol.id.as("alcoholId"),
//						alcohol.korName.as("korName"),
//						alcohol.engName.as("engName"),
//						alcohol.korCategory.as("korCategoryName"),
//						alcohol.imageUrl.as("imageUrl"),
//						picks.id.countDistinct().gt(0).as("isPicked"),
//				))
//				.from(alcohol)
//				.leftJoin(picks).on(picks.alcoholId.eq(alcohol.id).and(picks.userId.eq(userId)).and(picks.status.eq(PICK)))
//				.leftJoin(review).on(review.alcoholId.eq(alcohol.id).and(review.userId.eq(userId)).and(review.activeStatus.eq(ReviewActiveStatus.ACTIVE)))
//				.leftJoin(rating).on(rating.id.alcoholId.eq(alcohol.id).and(rating.id.userId.eq(userId)).and(rating.ratingPoint.rating.gt(0.0)))
//				.where(
//						picks.id.isNotNull().or(rating.id.isNotNull()).or(review.id.isNotNull()),
//						supporter.eqTabType(request.tabType()),
//						supporter.eqName(request.keyword()),
//						supporter.eqRegion(request.regionId())
//				)
//				.groupBy(alcohol.id, alcohol.korName, alcohol.engName, alcohol.korCategory, alcohol.imageUrl)
//				.orderBy(supporter.sortBy(request.sortType(), request.sortOrder()))
//				.offset(request.cursor())
//				.limit(request.pageSize() + 1)
//				.fetch();
//
//		CursorPageable cursorPageable = supporter.myBottleCursorPageable(request, myBottleList);
//		Long totalCount;
//		// 공통 조건을 추출한 베이스 쿼리 생성
//		JPAQuery<?> totalCountBaseQuery = queryFactory
//				.from(alcohol)
//				.leftJoin(picks).on(picks.alcoholId.eq(alcohol.id)
//						.and(picks.userId.eq(userId))
//						.and(picks.status.eq(PICK)))
//				.leftJoin(review).on(review.alcoholId.eq(alcohol.id)
//						.and(review.userId.eq(userId))
//						.and(review.activeStatus.eq(ReviewActiveStatus.ACTIVE)))
//				.leftJoin(rating).on(rating.id.alcoholId.eq(alcohol.id)
//						.and(rating.id.userId.eq(userId))
//						.and(rating.ratingPoint.rating.gt(0.0)))
//				.where(
//						picks.id.isNotNull().or(rating.id.isNotNull()).or(review.id.isNotNull()),
//						supporter.eqTabType(request.tabType()),
//						supporter.eqName(request.keyword()),
//						supporter.eqRegion(request.regionId())
//				);
//
//		if (ALL.equals(request.tabType())) {
//			// ALL인 경우는 각 카운트를 개별로 집계한 후 합산
//			Tuple counts = totalCountBaseQuery.select(
//					review.id.count().as("reviewCount"),
//					rating.id.countDistinct().as("ratingCount"),
//					picks.id.countDistinct().as("pickCount")
//			).fetchOne();
//
//			if (counts != null) {
//				final Long reviewCountValue = counts.get(0, Long.class);
//				final Long ratingCountValue = counts.get(1, Long.class);
//				final Long pickCountValue = counts.get(2, Long.class);
//
//				Long reviewCount = reviewCountValue != null ? reviewCountValue : 0L;
//				Long ratingCount = ratingCountValue != null ? ratingCountValue : 0L;
//				Long pickCount = pickCountValue != null ? pickCountValue : 0L;
//				totalCount = reviewCount + ratingCount + pickCount;
//				log.info("reviewCount : {}, ratingCount : {}, pickCount : {}", reviewCount, ratingCount, pickCount);
//			} else {
//				totalCount = 0L;
//			}
//		} else {
//			totalCount = totalCountBaseQuery.select(
//					request.tabType().equals(REVIEW) ? alcohol.id.count() : alcohol.id.countDistinct()
//			).fetchOne();
//		}
//		return MyBottleResponse.createMyBottle(
//				userId,
//				isMyPage,
//				totalCount,
//				myBottleList,
//				cursorPageable
//		);
//		return null;
//	}
	@Override
	public MyBottleResponse getReviewMyBottle(MyBottlePageableCriteria request) {
		Long userId = request.userId();
		boolean isMyPage = userId.equals(request.currentUserId());

		List<ReviewMyBottleItem> reviewMyBottleList = queryFactory
				.select(Projections.constructor(
						ReviewMyBottleItem.class,
						Projections.constructor(
								MyBottleResponse.BaseMyBottleInfo.class,
								alcohol.id.as("alcoholId"),
								alcohol.korName.as("alcoholKorName"),
								alcohol.engName.as("alcoholEngName"),
								alcohol.korCategory.as("korCategoryName"),
								alcohol.imageUrl.as("imageUrl"),
								supporter.isHot5(alcohol.id).as("isHot5")

						),
						review.id.as("reviewId"),
						review.id.isNotNull().as("isMyReview"),
						review.lastModifyAt.as("reviewModifyAt"),
						review.content.as("reviewContent"),
						Expressions.constant(Collections.emptySet()),
						review.isBest.as("isBestReview")
				))
				.from(alcohol)
				.leftJoin(review).on(review.alcoholId.eq(alcohol.id).and(review.userId.eq(userId)).and(review.activeStatus.eq(ReviewActiveStatus.ACTIVE)))
				.where(
						supporter.eqName(request.keyword()),
						supporter.eqRegion(request.regionId())
				)
				.groupBy(
						alcohol.id,
						alcohol.korName,
						alcohol.engName,
						alcohol.korCategory,
						alcohol.imageUrl,
						review.id,
						review.lastModifyAt,
						review.content,
						review.isBest
				)
				.orderBy(supporter.sortBy(MyBottleType.REVIEW, request.sortType(), request.sortOrder()))
				.offset(request.cursor())
				.limit(request.pageSize() + 1)
				.fetch();

		List<Long> reviewIds = reviewMyBottleList.stream()
				.map(ReviewMyBottleItem::reviewId)
				.toList();

		log.info("reviewIds : {}", reviewIds);

		// 3. 태그 조회
		QReviewTastingTag rtt = QReviewTastingTag.reviewTastingTag;

		Map<Long, Set<String>> reviewIdToTagsMap = queryFactory
				.select(rtt.review.id, rtt.tastingTag)
				.from(rtt)
				.where(rtt.review.id.in(reviewIds))
				.fetch()
				.stream()
				.collect(Collectors.groupingBy(
						tuple -> tuple.get(0, Long.class),
						Collectors.mapping(tuple -> tuple.get(1, String.class), Collectors.toSet())
				));

		log.info("reviewIdToTagsMap : {}", reviewIdToTagsMap);

		// 4. 태그 조립
		List<ReviewMyBottleItem> mergedReviewMyBottleList = reviewMyBottleList.stream()
				.map(r -> new ReviewMyBottleItem(
						r.baseMyBottleInfo(),
						r.reviewId(),
						r.isMyReview(),
						r.reviewModifyAt(),
						r.reviewContent(),
						reviewIdToTagsMap.getOrDefault(r.reviewId(), Collections.emptySet()),
						r.isBestReview()
				))
				.toList();

		log.info("mergedReviewMyBottleList : {}", mergedReviewMyBottleList);

		CursorPageable cursorPageable = supporter.myBottleCursorPageable(request, mergedReviewMyBottleList);
		Long totalCount;
		// 공통 조건을 추출한 베이스 쿼리 생성
		JPAQuery<?> totalCountBaseQuery = queryFactory
				.from(alcohol)
				.leftJoin(review).on(review.alcoholId.eq(alcohol.id)
						.and(review.userId.eq(userId))
						.and(review.activeStatus.eq(ReviewActiveStatus.ACTIVE)))
				.where(
						supporter.eqName(request.keyword()),
						supporter.eqRegion(request.regionId())
				);

		totalCount = totalCountBaseQuery.select(alcohol.id.count()).fetchOne();
		return MyBottleResponse.create(
				userId,
				isMyPage,
				totalCount,
				mergedReviewMyBottleList,
				cursorPageable
		);
	}

	@Override
	public MyBottleResponse getRatingMyBottle(MyBottlePageableCriteria request) {

		Long userId = request.userId();
		boolean isMyPage = userId.equals(request.currentUserId());

		List<RatingMyBottleItem> ratingMyBottleList = queryFactory
				.select(Projections.constructor(
						RatingMyBottleItem.class,
						Projections.constructor(
								MyBottleResponse.BaseMyBottleInfo.class,
								alcohol.id.as("alcoholId"),
								alcohol.korName.as("alcoholKorName"),
								alcohol.engName.as("alcoholEngName"),
								alcohol.korCategory.as("korCategoryName"),
								alcohol.imageUrl.as("imageUrl"),
								supporter.isHot5(alcohol.id).as("isHot5")
						),
						rating.ratingPoint.rating.as("myRatingPoint"),
						supporter.averageRatingSubQuery(alcohol.id),
						supporter.averageRatingCountSubQuery(alcohol.id),
						rating.lastModifyAt.as("ratingModifyAt")
				))
				.from(alcohol)
				.join(rating).on(rating.id.alcoholId.eq(alcohol.id)
						.and(rating.id.userId.eq(userId)))
				.where(
						supporter.eqName(request.keyword()),
						supporter.eqRegion(request.regionId())
				)
				.groupBy(
						alcohol.id,
						alcohol.korName,
						alcohol.engName,
						alcohol.korCategory,
						alcohol.imageUrl
				)
				.orderBy(supporter.sortBy(MyBottleType.RATING, request.sortType(), request.sortOrder()))
				.offset(request.cursor())
				.limit(request.pageSize() + 1)
				.fetch();
		CursorPageable cursorPageable = supporter.myBottleCursorPageable(request, ratingMyBottleList);
		Long totalCount;
		// 공통 조건을 추출한 베이스 쿼리 생성
		JPAQuery<?> totalCountBaseQuery = queryFactory
				.from(alcohol)
				.leftJoin(rating).on(rating.id.alcoholId.eq(alcohol.id))
				.where(
						supporter.eqName(request.keyword()),
						supporter.eqRegion(request.regionId())
				);

		totalCount = totalCountBaseQuery.select(alcohol.id.count()).fetchOne();
		return MyBottleResponse.create(
				userId,
				isMyPage,
				totalCount,
				ratingMyBottleList,
				cursorPageable
		);
	}
}
