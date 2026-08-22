package app.bottlenote.review.repository;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.like.domain.QLikes.likes;
import static app.bottlenote.review.constant.ReviewActiveStatus.ACTIVE;
import static app.bottlenote.review.constant.ReviewDisplayStatus.PUBLIC;
import static app.bottlenote.review.domain.QReview.review;
import static app.bottlenote.review.domain.QReviewImage.reviewImage;
import static app.bottlenote.review.domain.QReviewReply.reviewReply;
import static app.bottlenote.review.domain.QReviewTastingTag.reviewTastingTag;
import static app.bottlenote.review.repository.ReviewQuerySupporter.adminReviewFilters;
import static app.bottlenote.review.repository.ReviewQuerySupporter.adminReviewSortBy;
import static app.bottlenote.review.repository.ReviewQuerySupporter.containsKeywordInAll;
import static app.bottlenote.review.repository.ReviewQuerySupporter.cursorKeys;
import static app.bottlenote.review.repository.ReviewQuerySupporter.distinctLikesCount;
import static app.bottlenote.review.repository.ReviewQuerySupporter.getTastingTag;
import static app.bottlenote.review.repository.ReviewQuerySupporter.getUserInfo;
import static app.bottlenote.review.repository.ReviewQuerySupporter.hasReplyByMeSubquery;
import static app.bottlenote.review.repository.ReviewQuerySupporter.isLikeByMeSubquery;
import static app.bottlenote.review.repository.ReviewQuerySupporter.isMyReview;
import static app.bottlenote.review.repository.ReviewQuerySupporter.keysetSeek;
import static app.bottlenote.review.repository.ReviewQuerySupporter.sortBy;
import static app.bottlenote.user.domain.QUser.user;

import app.bottlenote.global.pagination.HmacCursorCodec;
import app.bottlenote.global.pagination.KeysetPageResponse;
import app.bottlenote.global.pagination.KeysetPagination;
import app.bottlenote.like.constant.LikeStatus;
import app.bottlenote.review.dto.request.AdminReviewSearchRequest;
import app.bottlenote.review.dto.request.ReviewPageableRequest;
import app.bottlenote.review.dto.dsl.ReviewExploreCriteria;
import app.bottlenote.review.dto.response.AdminReviewListResponse;
import app.bottlenote.review.dto.response.ReviewExploreItem;
import app.bottlenote.review.dto.response.ReviewExploreListResponse;
import app.bottlenote.review.dto.response.ReviewListResponse;
import app.bottlenote.review.facade.payload.LocationInfo;
import app.bottlenote.review.facade.payload.ReviewInfo;
import app.bottlenote.review.facade.payload.UserInfo;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@Slf4j
@RequiredArgsConstructor
public class CustomReviewRepositoryImpl implements CustomReviewRepository {

  private final JPAQueryFactory queryFactory;
  private final HmacCursorCodec cursorCodec;

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
        review.reviewLocation,
        review.sizeType,

        // 가격 및 평점 정보
        review.price,
        review.reviewRating,

        // 좋아요 및 댓글 정보
        distinctLikesCount(),
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
  public KeysetPageResponse<ReviewListResponse> getReviews(
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
            .having(
                reviewSeek(
                    reviewPageableRequest, reviewContext(alcoholId, userId, reviewPageableRequest)))
            .limit(reviewPageableRequest.size() + 1L)
            .fetch();

    return toReviewPage(
        fetch, reviewPageableRequest, reviewContext(alcoholId, userId, reviewPageableRequest));
  }

  @Override
  public KeysetPageResponse<ReviewListResponse> getReviewsByMe(
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
            .having(
                reviewSeek(
                    reviewPageableRequest, reviewContext(alcoholId, userId, reviewPageableRequest)))
            .limit(reviewPageableRequest.size() + 1L)
            .fetch();

    return toReviewPage(
        fetch, reviewPageableRequest, reviewContext(alcoholId, userId, reviewPageableRequest));
  }

  private KeysetPageResponse<ReviewListResponse> toReviewPage(
      List<ReviewInfo> fetch, ReviewPageableRequest request, String context) {
    var slice =
        KeysetPagination.fromOverflow(
            fetch,
            request.size(),
            item -> cursorCodec.encode(context, cursorKeys(request.sortType(), item)));
    return KeysetPageResponse.of(ReviewListResponse.of(slice.items()), slice.pagination());
  }

  private com.querydsl.core.types.dsl.BooleanExpression reviewSeek(
      ReviewPageableRequest request, String context) {
    if (request.cursor() == null) {
      return null;
    }
    var claims = cursorCodec.verify(request.cursor(), context);
    return keysetSeek(request.sortType(), request.sortOrder(), claims);
  }

  private static String reviewContext(Long alcoholId, Long userId, ReviewPageableRequest request) {
    return "review.list:"
        + alcoholId
        + ":"
        + userId
        + ":"
        + request.sortType()
        + ":"
        + request.sortOrder();
  }

  @Override
  public Page<AdminReviewListResponse> searchAdminReviews(AdminReviewSearchRequest request) {
    List<AdminReviewListResponse> content =
        queryFactory
            .select(
                Projections.constructor(
                    AdminReviewListResponse.class,
                    review.id,
                    alcohol.id,
                    alcohol.korName,
                    user.id,
                    user.nickName,
                    review.content,
                    review.reviewRating,
                    review.activeStatus,
                    review.status,
                    reviewReply.id.countDistinct(),
                    review.createAt,
                    review.lastModifyAt))
            .from(review)
            .join(user)
            .on(review.userId.eq(user.id))
            .leftJoin(alcohol)
            .on(alcohol.id.eq(review.alcoholId))
            .leftJoin(reviewReply)
            .on(review.id.eq(reviewReply.reviewId))
            .where(adminReviewFilters(request))
            .groupBy(
                review.id,
                alcohol.id,
                alcohol.korName,
                user.id,
                user.nickName,
                review.content,
                review.reviewRating,
                review.activeStatus,
                review.status,
                review.createAt,
                review.lastModifyAt)
            .orderBy(
                adminReviewSortBy(request.sortType(), request.sortOrder())
                    .toArray(new OrderSpecifier[0]))
            .offset((long) request.page() * request.size())
            .limit(request.size())
            .fetch();

    Long total =
        queryFactory
            .select(review.id.countDistinct())
            .from(review)
            .join(user)
            .on(review.userId.eq(user.id))
            .leftJoin(alcohol)
            .on(alcohol.id.eq(review.alcoholId))
            .where(adminReviewFilters(request))
            .fetchOne();

    return new PageImpl<>(
        content, PageRequest.of(request.page(), request.size()), total != null ? total : 0L);
  }

  @Override
  public KeysetPageResponse<ReviewExploreListResponse> getStandardExplore(
      ReviewExploreCriteria criteria) {
    Long userId = criteria.userId();
    int size = criteria.size();
    int fetchSize = size + 1;
    String context = criteria.context();
    var cursorClaims =
        criteria.cursor() == null ? null : cursorCodec.verify(criteria.cursor(), context);

    StringExpression groupConcatImages =
        Expressions.stringTemplate(
            "GROUP_CONCAT(DISTINCT {0})", reviewImage.reviewImageInfo.imageUrl);
    StringExpression groupConcatTags =
        Expressions.stringTemplate("GROUP_CONCAT(DISTINCT {0})", reviewTastingTag.tastingTag);

    List<Tuple> results =
        queryFactory
            .select(
                user.id,
                user.nickName,
                user.imageUrl,
                user.id.eq(userId),
                alcohol.id,
                alcohol.korName,
                review.id,
                review.content,
                review.reviewRating,
                review.sizeType,
                review.price,
                groupConcatTags,
                review.createAt,
                review.lastModifyAt,
                reviewImage.id.countDistinct(),
                groupConcatImages,

                // 저장된 리뷰 위치 정보
                review.reviewLocation.name,
                review.reviewLocation.zipCode,
                review.reviewLocation.address,
                review.reviewLocation.detailAddress,
                review.reviewLocation.category,
                review.reviewLocation.mapUrl,
                review.reviewLocation.latitude,
                review.reviewLocation.longitude,

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
            .leftJoin(reviewImage)
            .on(review.id.eq(reviewImage.review.id))
            .leftJoin(reviewReply)
            .on(review.id.eq(reviewReply.reviewId))
            .leftJoin(reviewTastingTag)
            .on(review.id.eq(reviewTastingTag.review.id))
            .where(
                review.activeStatus.eq(ACTIVE),
                review.status.eq(PUBLIC),
                containsKeywordInAll(criteria.effectiveKeywords()),
                criteria.ratingFrom() == null
                    ? null
                    : review.reviewRating.goe(criteria.ratingFrom().doubleValue()),
                criteria.ratingTo() == null
                    ? null
                    : review.reviewRating.loe(criteria.ratingTo().doubleValue()))
            .groupBy(
                review.id,
                review.content,
                review.reviewRating,
                review.sizeType,
                review.price,
                review.createAt,
                review.lastModifyAt,
                review.isBest,
                user.id,
                user.nickName,
                user.imageUrl,
                alcohol.id,
                alcohol.korName,
                review.reviewLocation.name,
                review.reviewLocation.zipCode,
                review.reviewLocation.address,
                review.reviewLocation.detailAddress,
                review.reviewLocation.category,
                review.reviewLocation.mapUrl,
                review.reviewLocation.latitude,
                review.reviewLocation.longitude)
            .having(keysetSeek(criteria.sortType(), criteria.sortOrder(), cursorClaims))
            .orderBy(sortBy(criteria.sortType(), criteria.sortOrder()).toArray(new OrderSpecifier[0]))
            .limit(fetchSize)
            .fetch();

    List<ReviewExploreItem> items = new ArrayList<>();
    Map<Long, ReviewInfo> cursorItems = new HashMap<>();
    for (Tuple tuple : results) {
      UserInfo userInfo =
          UserInfo.of(tuple.get(user.id), tuple.get(user.nickName), tuple.get(user.imageUrl));
      String tagStr = tuple.get(groupConcatTags);
      List<String> tags =
          tagStr != null ? new ArrayList<>(Arrays.asList(tagStr.split(","))) : new ArrayList<>();
      String imageUrlsStr = tuple.get(groupConcatImages);
      List<String> imageUrls =
          imageUrlsStr != null
              ? new ArrayList<>(Arrays.asList(imageUrlsStr.split(",")))
              : new ArrayList<>();

      LocationInfo locationInfo =
          LocationInfo.of(
              tuple.get(review.reviewLocation.name),
              tuple.get(review.reviewLocation.zipCode),
              tuple.get(review.reviewLocation.address),
              tuple.get(review.reviewLocation.detailAddress),
              tuple.get(review.reviewLocation.category),
              tuple.get(review.reviewLocation.mapUrl),
              tuple.get(review.reviewLocation.latitude),
              tuple.get(review.reviewLocation.longitude));

      ReviewExploreItem item =
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
              locationInfo,
              tuple.get(review.isBest),
              tuple.get(likes.id.countDistinct()),
              tuple.get(isLikeByMeSubquery(userId)),
              tuple.get(reviewReply.id.countDistinct()),
              tuple.get(hasReplyByMeSubquery(userId)));
      items.add(item);
      cursorItems.put(
          item.reviewId(),
          ReviewInfo.builder()
              .reviewId(item.reviewId())
              .createAt(item.createAt())
              .isBestReview(item.isBestReview())
              .sizeType(tuple.get(review.sizeType))
              .price(tuple.get(review.price))
              .rating(item.reviewRating())
              .likeCount(item.likeCount())
              .build());
    }

    var slice =
        KeysetPagination.fromOverflow(
            items,
            size,
            item ->
                cursorCodec.encode(context, cursorKeys(criteria.sortType(), cursorItems.get(item.reviewId()))));
    return KeysetPageResponse.of(new ReviewExploreListResponse(slice.items()), slice.pagination());
  }

}
