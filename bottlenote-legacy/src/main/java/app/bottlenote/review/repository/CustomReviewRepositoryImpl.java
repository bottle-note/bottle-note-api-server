package app.bottlenote.review.repository;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.like.domain.QLikes.likes;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.domain.QReview.review;
import static app.bottlenote.review.domain.QReviewImage.reviewImage;
import static app.bottlenote.review.domain.QReviewReply.reviewReply;
import static app.bottlenote.review.domain.QReviewTastingTag.reviewTastingTag;
import static app.bottlenote.review.repository.ReviewQuerySupporter.containsKeywordInAll;
import static app.bottlenote.review.repository.ReviewQuerySupporter.getCursorPageable;
import static app.bottlenote.review.repository.ReviewQuerySupporter.getTastingTag;
import static app.bottlenote.review.repository.ReviewQuerySupporter.getUserInfo;
import static app.bottlenote.review.repository.ReviewQuerySupporter.hasReplyByMeSubquery;
import static app.bottlenote.review.repository.ReviewQuerySupporter.isLikeByMeSubquery;
import static app.bottlenote.review.repository.ReviewQuerySupporter.isMyReview;
import static app.bottlenote.review.repository.ReviewQuerySupporter.sortBy;
import static app.bottlenote.shared.review.constant.ReviewActiveStatus.ACTIVE;
import static app.bottlenote.shared.review.constant.ReviewDisplayStatus.PUBLIC;
import static app.bottlenote.user.domain.QUser.user;

import app.bottlenote.like.constant.LikeStatus;
import app.bottlenote.review.dto.request.ReviewPageableRequest;
import app.bottlenote.review.dto.response.ReviewExploreItem;
import app.bottlenote.review.dto.response.ReviewListResponse;
import app.bottlenote.shared.cursor.CursorPageable;
import app.bottlenote.shared.cursor.CursorResponse;
import app.bottlenote.shared.cursor.PageResponse;
import app.bottlenote.shared.review.payload.LocationInfo;
import app.bottlenote.shared.review.payload.ReviewInfo;
import app.bottlenote.shared.review.payload.UserInfo;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

@Slf4j
@RequiredArgsConstructor
public class CustomReviewRepositoryImpl implements CustomReviewRepository {

  private final JPAQueryFactory queryFactory;

  private static ConstructorExpression<ReviewInfo> composeReviewInfoResult(Long userId) {
    return Projections.constructor(
        ReviewInfo.class,
        // 기본 리뷰 정보
        review.id,
        review.content,
        review.imageUrl,
        review.createAt,
        reviewImage.id.countDistinct(),

        // 사용자 정보
        getUserInfo(),
        isMyReview(userId),

        // 리뷰 상태 및 속성
        review.status,
        review.isBest,
        // LocationInfo 객체를 QueryDSL로 생성
        Projections.constructor(
            LocationInfo.class,
            review.reviewLocation.name,
            review.reviewLocation.zipCode,
            review.reviewLocation.address,
            review.reviewLocation.detailAddress,
            review.reviewLocation.category,
            review.reviewLocation.mapUrl,
            review.reviewLocation.latitude,
            review.reviewLocation.longitude),
        review.sizeType,

        // 가격 및 평점 정보
        review.price,
        rating.ratingPoint.rating,

        // 좋아요 및 댓글 정보
        likes.countDistinct(),
        reviewReply.countDistinct(),
        isLikeByMeSubquery(userId),
        hasReplyByMeSubquery(userId),

        // 기타 정보
        review.viewCount,
        getTastingTag());
  }

  @Override
  public ReviewInfo getReview(Long reviewId, Long userId) {
    return queryFactory
        .select(composeReviewInfoResult(userId))
        .from(review)
        .join(user)
        .on(review.userId.eq(user.id))
        .leftJoin(likes)
        .on(review.id.eq(likes.reviewId).and(likes.status.eq(LikeStatus.LIKE)))
        .leftJoin(alcohol)
        .on(alcohol.id.eq(review.alcoholId))
        .leftJoin(rating)
        .on(rating.id.alcoholId.eq(review.alcoholId).and(rating.id.userId.eq(review.userId)))
        .leftJoin(reviewImage)
        .on(review.id.eq(reviewImage.review.id))
        .leftJoin(reviewReply)
        .on(review.id.eq(reviewReply.reviewId))
        .where(
            review
                .id
                .eq(reviewId)
                .and(review.userId.eq(userId).or(review.status.eq(PUBLIC)))
                .and(review.activeStatus.eq(ACTIVE)))
        .groupBy(review.id, review.isBest, review.sizeType, review.userId)
        .fetchOne();
  }

  @Override
  public PageResponse<ReviewListResponse> getReviews(
      Long alcoholId, ReviewPageableRequest reviewPageableRequest, Long userId) {
    List<ReviewInfo> fetch =
        queryFactory
            .select(composeReviewInfoResult(userId))
            .from(review)
            .join(user)
            .on(review.userId.eq(user.id))
            .leftJoin(likes)
            .on(review.id.eq(likes.reviewId).and(likes.status.eq(LikeStatus.LIKE)))
            .leftJoin(alcohol)
            .on(alcohol.id.eq(review.alcoholId))
            .leftJoin(rating)
            .on(rating.id.alcoholId.eq(review.alcoholId).and(rating.id.userId.eq(review.userId)))
            .leftJoin(reviewReply)
            .on(review.id.eq(reviewReply.reviewId))
            .leftJoin(reviewImage)
            .on(review.id.eq(reviewImage.review.id))
            .where(
                review
                    .alcoholId
                    .eq(alcoholId)
                    .and(
                        review
                            .userId
                            .eq(userId)
                            .or(review.status.eq(PUBLIC))) // 내 리뷰는 모두 조회 아니면 공개된 리뷰만 조회
                    .and(review.activeStatus.eq(ACTIVE)))
            .groupBy(review.id, review.isBest, review.sizeType, review.userId)
            .orderBy(
                sortBy(reviewPageableRequest.sortType(), reviewPageableRequest.sortOrder())
                    .toArray(new OrderSpecifier[0]))
            .offset(reviewPageableRequest.cursor())
            .limit(reviewPageableRequest.pageSize() + 1)
            .fetch();

    Long totalCount =
        queryFactory
            .select(review.id.count())
            .from(review)
            .where(
                review
                    .alcoholId
                    .eq(alcoholId)
                    .and(
                        review
                            .userId
                            .eq(userId)
                            .or(review.status.eq(PUBLIC))) // 내 리뷰는 모두 조회 아니면 공개된 리뷰만 조회
                    .and(review.activeStatus.eq(ACTIVE)))
            .fetchOne();

    CursorPageable cursorPageable = getCursorPageable(reviewPageableRequest, fetch);
    return PageResponse.of(ReviewListResponse.of(totalCount, fetch), cursorPageable);
  }

  @Override
  public PageResponse<ReviewListResponse> getReviewsByMe(
      Long alcoholId, ReviewPageableRequest reviewPageableRequest, Long userId) {
    // 특정한 위스키의 내 리뷰만 조회
    List<ReviewInfo> fetch =
        queryFactory
            .select(composeReviewInfoResult(userId))
            .from(review)
            .join(alcohol)
            .on(alcohol.id.eq(review.alcoholId).and(alcohol.id.eq(alcoholId)))
            .join(user)
            .on(review.userId.eq(user.id))
            .leftJoin(likes)
            .on(review.id.eq(likes.reviewId).and(likes.status.eq(LikeStatus.LIKE)))
            .leftJoin(rating)
            .on(rating.id.alcoholId.eq(review.alcoholId).and(rating.id.userId.eq(review.userId)))
            .leftJoin(reviewReply)
            .on(review.id.eq(reviewReply.reviewId))
            .leftJoin(reviewImage)
            .on(review.id.eq(reviewImage.review.id))
            .where(review.userId.eq(userId).and(review.activeStatus.eq(ACTIVE)))
            // .and(review.status.eq(PUBLIC)))// 공개 여부와 상관 없이 모두 조회
            .groupBy(review.id, review.isBest, review.sizeType, review.userId)
            .orderBy(
                sortBy(reviewPageableRequest.sortType(), reviewPageableRequest.sortOrder())
                    .toArray(new OrderSpecifier[0]))
            .offset(reviewPageableRequest.cursor())
            .limit(reviewPageableRequest.pageSize() + 1)
            .fetch();

    Long totalCount =
        queryFactory
            .select(review.id.count())
            .from(review)
            .where(
                review
                    .userId
                    .eq(userId)
                    .and(review.alcoholId.eq(alcoholId))
                    .and(review.activeStatus.eq(ACTIVE))) //
            // .and(review.status.eq(PUBLIC))) // 공개 여부와 상관 없이 모두 조회
            .fetchOne();

    CursorPageable cursorPageable = getCursorPageable(reviewPageableRequest, fetch);
    return PageResponse.of(ReviewListResponse.of(totalCount, fetch), cursorPageable);
  }

  @Override
  public Pair<Long, CursorResponse<ReviewExploreItem>> getStandardExplore(
      Long userId, List<String> keywords, Long cursor, Integer size) {
    int fetchSize = size + 1;

    // GROUP_CONCAT 표현식
    StringExpression groupConcatImages =
        Expressions.stringTemplate(
            "GROUP_CONCAT(DISTINCT {0})", reviewImage.reviewImageInfo.imageUrl);

    StringExpression groupConcatTags =
        Expressions.stringTemplate("GROUP_CONCAT(DISTINCT {0})", reviewTastingTag.tastingTag);

    // Tuple로 결과 가져오기
    List<Tuple> results =
        queryFactory
            .select(
                // 사용자 정보
                user.id,
                user.nickName,
                user.imageUrl,
                user.id.eq(userId),

                // 주류 정보
                alcohol.id,
                alcohol.korName,

                // 리뷰 정보
                review.id,
                review.content,
                review.reviewRating,
                groupConcatTags,
                review.createAt,
                review.lastModifyAt,
                reviewImage.id.countDistinct(),
                groupConcatImages,

                // 상태 및 속성
                review.isBest,
                likes.id.countDistinct(),
                isLikeByMeSubquery(userId),
                reviewReply.id.countDistinct(),
                hasReplyByMeSubquery(userId))
            .from(review)
            .join(user)
            .on(review.userId.eq(user.id))
            .leftJoin(alcohol)
            .on(alcohol.id.eq(review.alcoholId))
            .leftJoin(likes)
            .on(review.id.eq(likes.reviewId).and(likes.status.eq(LikeStatus.LIKE)))
            .leftJoin(rating)
            .on(rating.id.alcoholId.eq(review.alcoholId).and(rating.id.userId.eq(review.userId)))
            .leftJoin(reviewImage)
            .on(review.id.eq(reviewImage.review.id))
            .leftJoin(reviewReply)
            .on(review.id.eq(reviewReply.reviewId))
            .leftJoin(reviewTastingTag)
            .on(review.id.eq(reviewTastingTag.review.id))
            .where(
                review.activeStatus.eq(ACTIVE),
                review.status.eq(PUBLIC),
                containsKeywordInAll(keywords))
            .groupBy(
                review.id,
                review.content,
                review.reviewRating,
                review.createAt,
                review.lastModifyAt,
                review.isBest,
                user.id,
                user.nickName,
                user.imageUrl,
                alcohol.id,
                alcohol.korName)
            .orderBy(review.lastModifyAt.desc())
            .offset(cursor)
            .limit(fetchSize)
            .fetch();

    // Tuple 결과를 ReviewExploreItem으로 변환
    List<ReviewExploreItem> items = new ArrayList<>();

    for (Tuple tuple : results) {
      // UserInfo 생성
      UserInfo userInfo =
          UserInfo.of(tuple.get(user.id), tuple.get(user.nickName), tuple.get(user.imageUrl));

      // 태그 문자열을 List<String>으로 변환
      String tagStr = tuple.get(groupConcatTags);
      List<String> tags =
          tagStr != null ? new ArrayList<>(Arrays.asList(tagStr.split(","))) : new ArrayList<>();

      // 이미지 URL 문자열을 List<String>으로 변환
      String imageUrlsStr = tuple.get(groupConcatImages);
      List<String> imageUrls =
          imageUrlsStr != null
              ? new ArrayList<>(Arrays.asList(imageUrlsStr.split(",")))
              : new ArrayList<>();

      // ReviewExploreItem 생성
      items.add(
          new ReviewExploreItem(
              userInfo,
              tuple.get(user.id.eq(userId)),
              tuple.get(alcohol.id),
              tuple.get(alcohol.korName),
              tuple.get(review.id),
              tuple.get(review.content),
              tuple.get(review.reviewRating),
              tags,
              tuple.get(review.createAt),
              tuple.get(review.lastModifyAt),
              tuple.get(reviewImage.id.countDistinct()),
              imageUrls,
              tuple.get(review.isBest),
              tuple.get(likes.id.countDistinct()),
              tuple.get(isLikeByMeSubquery(userId)),
              tuple.get(reviewReply.id.countDistinct()),
              tuple.get(hasReplyByMeSubquery(userId))));
    }

    // 총 개수 조회
    Long total =
        queryFactory
            .select(review.countDistinct())
            .from(review)
            .join(user)
            .on(review.userId.eq(user.id))
            .leftJoin(alcohol)
            .on(alcohol.id.eq(review.alcoholId))
            .leftJoin(likes)
            .on(review.id.eq(likes.reviewId).and(likes.status.eq(LikeStatus.LIKE)))
            .leftJoin(rating)
            .on(rating.id.alcoholId.eq(review.alcoholId).and(rating.id.userId.eq(review.userId)))
            .leftJoin(reviewImage)
            .on(review.id.eq(reviewImage.review.id))
            .leftJoin(reviewReply)
            .on(review.id.eq(reviewReply.reviewId))
            .leftJoin(reviewTastingTag)
            .on(review.id.eq(reviewTastingTag.review.id))
            .where(
                review.activeStatus.eq(ACTIVE),
                review.status.eq(PUBLIC),
                containsKeywordInAll(keywords))
            .fetchOne();

    // 직접 가변 리스트를 사용
    CursorResponse<ReviewExploreItem> list = CursorResponse.of(items, cursor, size);
    return Pair.of(total, list);
  }
}
