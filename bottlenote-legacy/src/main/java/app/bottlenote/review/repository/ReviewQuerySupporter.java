package app.bottlenote.review.repository;

import static app.bottlenote.alcohols.domain.QAlcohol.alcohol;
import static app.bottlenote.like.constant.LikeStatus.LIKE;
import static app.bottlenote.like.domain.QLikes.likes;
import static app.bottlenote.rating.domain.QRating.rating;
import static app.bottlenote.review.constant.ReviewReplyStatus.NORMAL;
import static app.bottlenote.review.domain.QReview.review;
import static app.bottlenote.review.domain.QReviewReply.reviewReply;
import static app.bottlenote.review.domain.QReviewTastingTag.reviewTastingTag;
import static app.bottlenote.shared.cursor.SortOrder.DESC;
import static app.bottlenote.user.domain.QUser.user;
import static com.querydsl.jpa.JPAExpressions.select;

import app.bottlenote.review.constant.ReviewActiveStatus;
import app.bottlenote.review.constant.ReviewSortType;
import app.bottlenote.review.dto.request.ReviewPageableRequest;
import app.bottlenote.review.facade.payload.ReviewInfo;
import app.bottlenote.review.facade.payload.UserInfo;
import app.bottlenote.shared.cursor.CursorPageable;
import app.bottlenote.shared.cursor.SortOrder;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.util.StringUtils;
import com.querydsl.jpa.JPAExpressions;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class ReviewQuerySupporter {

  public static ConstructorExpression<UserInfo> getUserInfo() {
    return Projections.constructor(
        UserInfo.class,
        user.id.as("userId"),
        user.nickName.as("nickName"),
        user.imageUrl.as("userProfileImage"));
  }

  public static Expression<String> getTastingTag() {
    return ExpressionUtils.as(
        JPAExpressions.select(
                Expressions.stringTemplate("group_concat({0})", reviewTastingTag.tastingTag))
            .from(reviewTastingTag)
            .where(reviewTastingTag.review.id.eq(review.id)),
        "tastingTag");
  }

  /** 내가 댓글을 단 리뷰인지 판별 */
  public static BooleanExpression hasReplyByMeSubquery(Long userId) {

    BooleanExpression eqUserId =
        1 > userId ? reviewReply.userId.isNull() : reviewReply.userId.eq(userId);

    return Expressions.asBoolean(
            JPAExpressions.selectOne()
                .from(reviewReply)
                .where(
                    reviewReply
                        .reviewId
                        .eq(review.id)
                        .and(eqUserId.and(reviewReply.status.eq(NORMAL))))
                .exists())
        .as("hasReplyByMe");
  }

  /***
   * 내가 좋아요를 누른 리뷰인지 판별
   */
  public static BooleanExpression isLikeByMeSubquery(Long userId) {
    if (userId < 1) {
      return Expressions.asBoolean(false);
    }
    return Expressions.asBoolean(
            JPAExpressions.selectOne()
                .from(likes)
                .where(
                    likes
                        .reviewId
                        .eq(review.id)
                        .and(likes.userInfo.userId.eq(userId))
                        .and(likes.status.eq(LIKE)))
                .exists())
        .as("isLikedByMe");
  }

  /***
   * 내가 작성한 리뷰인지 판별
   */
  public static BooleanExpression isMyReview(Long userId) {
    if (Objects.isNull(userId) || 1 > userId) {
      return Expressions.asBoolean(false);
    }
    return review.userId.eq(userId).as("isMyReview");
  }

  public static CursorPageable getCursorPageable(
      ReviewPageableRequest reviewPageableRequest, List<ReviewInfo> fetch) {

    boolean hasNext = isHasNext(reviewPageableRequest, fetch);
    return CursorPageable.builder()
        .cursor(reviewPageableRequest.cursor() + reviewPageableRequest.pageSize())
        .pageSize(reviewPageableRequest.pageSize())
        .hasNext(hasNext)
        .currentCursor(reviewPageableRequest.cursor())
        .build();
  }

  /** 다음 페이지가 있는지 확인하는 메소드 */
  public static boolean isHasNext(
      ReviewPageableRequest reviewPageableRequest, List<ReviewInfo> fetch) {
    boolean hasNext = fetch.size() > reviewPageableRequest.pageSize();
    if (hasNext) {
      fetch.remove(fetch.size() - 1); // Remove the extra record
    }
    return hasNext;
  }

  public static List<OrderSpecifier<?>> sortBy(ReviewSortType reviewSortType, SortOrder sortOrder) {
    NumberExpression<Long> likesCount = likes.id.count();
    return switch (reviewSortType) {
      // 인기순 -> 임시로 좋아요 순으로 구현
      case POPULAR ->
          Arrays.asList(
              new OrderSpecifier<>(sortOrder == DESC ? Order.DESC : Order.ASC, review.isBest)
                  .nullsLast(),
              new OrderSpecifier<>(sortOrder == DESC ? Order.DESC : Order.ASC, likesCount)
                  .nullsLast());
      // 좋아요 순
      case LIKES ->
          Collections.singletonList(sortOrder == DESC ? likesCount.desc() : likesCount.asc());

      // 별점 순
      case RATING ->
          Collections.singletonList(
              sortOrder == DESC
                  ? rating.ratingPoint.rating.desc()
                  : rating.ratingPoint.rating.asc());

      // 병 기준 가격 순
      case BOTTLE_PRICE -> {
        OrderSpecifier<?> sizeOrderSpecifier =
            new OrderSpecifier<>(Order.ASC, review.sizeType).nullsLast();

        OrderSpecifier<?> priceOrderSpecifier =
            new OrderSpecifier<>(sortOrder == DESC ? Order.DESC : Order.ASC, review.price);
        yield Arrays.asList(sizeOrderSpecifier, priceOrderSpecifier);
      }

      // 잔 기준 가격 순
      case GLASS_PRICE -> {
        OrderSpecifier<?> sizeOrderSpecifier =
            new OrderSpecifier<>(Order.DESC, review.sizeType).nullsLast();

        OrderSpecifier<?> priceOrderSpecifier =
            new OrderSpecifier<>(sortOrder == DESC ? Order.DESC : Order.ASC, review.price);
        yield Arrays.asList(sizeOrderSpecifier, priceOrderSpecifier);
      }
    };
  }

  /** 키워드를 이용해 작성자, 주류 정보, 리뷰 콘텐츠, 테이스팅 태그를 모두 검색하는 조건 생성 */
  public static BooleanExpression containsKeywordInAll(List<String> keywords) {
    if (keywords == null || keywords.isEmpty()) {
      return null;
    }
    BooleanExpression finalCondition = null;

    // 각 키워드에 대해 개별 조건을 생성하고 AND 연산으로 결합
    for (String keyword : keywords) {
      if (StringUtils.isNullOrEmpty(keyword)) {
        continue; // 빈 키워드는 건너뛰기
      }
      // 현재 키워드에 대한 조건들
      BooleanExpression keywordCondition =
          // 작성자 이름 검색
          user.nickName
              .likeIgnoreCase("%" + keyword + "%")
              // 술 정보 검색
              .or(alcohol.korName.likeIgnoreCase("%" + keyword + "%"))
              .or(alcohol.engName.likeIgnoreCase("%" + keyword + "%"))
              // 리뷰 콘텐츠 검색
              .or(review.content.likeIgnoreCase("%" + keyword + "%"));

      // 리뷰 테이스팅 태그 검색 조건
      BooleanExpression reviewTastingTagCondition =
          JPAExpressions.selectOne()
              .from(reviewTastingTag)
              .where(
                  reviewTastingTag.review.id.eq(review.id),
                  reviewTastingTag.tastingTag.likeIgnoreCase("%" + keyword + "%"))
              .exists();

      // 키워드 조건에 리뷰 테이스팅 태그 조건 추가
      keywordCondition = keywordCondition.or(reviewTastingTagCondition);

      // 결과 조건에 AND로 결합
      if (finalCondition == null) {
        finalCondition = keywordCondition;
      } else {
        finalCondition = finalCondition.and(keywordCondition);
      }
    }

    return finalCondition;
  }

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
            .where(review.userId.eq(userId).and(review.activeStatus.eq(ReviewActiveStatus.ACTIVE))),
        "reviewCount");
  }
}
