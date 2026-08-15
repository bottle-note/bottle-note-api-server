package app.bottlenote.review.repository;

import static app.bottlenote.like.domain.QLikes.likes;
import static app.bottlenote.review.domain.QReview.review;
import static app.bottlenote.review.repository.ReviewQuerySupporter.cursorKeys;
import static app.bottlenote.review.repository.ReviewQuerySupporter.keysetSeek;
import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.global.pagination.CursorClaims;
import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.review.constant.ReviewSortType;
import app.bottlenote.review.constant.SizeType;
import app.bottlenote.review.facade.payload.ReviewInfo;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("ReviewQuerySupporter의 keyset seek/cursor 순수 로직 단위 테스트")
class ReviewQuerySupporterTest {

  private static final LocalDateTime CREATE_AT = LocalDateTime.parse("2026-08-15T00:00:00");

  private static CursorClaims claimsOf(Map<String, String> sortKeys) {
    return new CursorClaims(1, "v1", "hash", sortKeys, null, null, Map.of());
  }

  private static ReviewInfo reviewInfo(
      Long reviewId,
      Boolean isBest,
      Long likeCount,
      Double rating,
      SizeType sizeType,
      BigDecimal price) {
    return ReviewInfo.builder()
        .reviewId(reviewId)
        .createAt(CREATE_AT)
        .isBestReview(isBest)
        .likeCount(likeCount)
        .rating(rating)
        .sizeType(sizeType)
        .price(price)
        .build();
  }

  @Nested
  @DisplayName("cursorKeys")
  class CursorKeysTest {

    @Test
    @DisplayName("POPULAR 정렬일 때 best, likes, t, id를 모두 커서에 담는다")
    void popular_담을_때_best와_likes를_함께_담는다() {
      // given
      ReviewInfo item = reviewInfo(2L, true, 2L, null, null, null);

      // when
      Map<String, String> keys = cursorKeys(ReviewSortType.POPULAR, item);

      // then
      assertThat(keys)
          .containsEntry("best", "true")
          .containsEntry("likes", "2")
          .containsEntry("t", CREATE_AT.toString())
          .containsEntry("id", "2");
    }

    @Test
    @DisplayName("LIKES 정렬에서 likeCount가 null이면 0으로 커서에 담는다")
    void likes_정렬에서_likeCount가_null이면_0으로_담는다() {
      // given
      ReviewInfo item = reviewInfo(3L, null, null, null, null, null);

      // when
      Map<String, String> keys = cursorKeys(ReviewSortType.LIKES, item);

      // then
      assertThat(keys).containsEntry("likes", "0");
    }

    @Test
    @DisplayName("POPULAR 정렬에서 likeCount가 null이어도 0으로 커서에 담는다")
    void popular_정렬에서_likeCount가_null이면_0으로_담는다() {
      // given
      ReviewInfo item = reviewInfo(4L, false, null, null, null, null);

      // when
      Map<String, String> keys = cursorKeys(ReviewSortType.POPULAR, item);

      // then : COUNT는 SQL에서 NULL이 아니라 0이라 키를 생략하면 seek이 아무 행도 못 찾는다
      assertThat(keys).containsEntry("likes", "0");
    }

    @Test
    @DisplayName("RATING 정렬에서 평점이 null이면 커서에 rating 키를 넣지 않는다")
    void rating_정렬에서_평점이_null이면_키를_생략한다() {
      // given
      ReviewInfo item = reviewInfo(5L, null, null, null, null, null);

      // when
      Map<String, String> keys = cursorKeys(ReviewSortType.RATING, item);

      // then
      assertThat(keys).doesNotContainKey("rating").containsEntry("t", CREATE_AT.toString());
    }

    @Test
    @DisplayName("BOTTLE_PRICE 정렬일 때 sizeType과 price를 함께 커서에 담는다")
    void bottle_price_정렬일_때_sizeType과_price를_담는다() {
      // given
      ReviewInfo item = reviewInfo(7L, null, null, null, SizeType.BOTTLE, new BigDecimal("10000"));

      // when
      Map<String, String> keys = cursorKeys(ReviewSortType.BOTTLE_PRICE, item);

      // then
      assertThat(keys).containsEntry("sizeType", "BOTTLE").containsEntry("price", "10000");
    }
  }

  @Nested
  @DisplayName("keysetSeek")
  class KeysetSeekTest {

    @Test
    @DisplayName("커서가 없으면 null을 반환한다")
    void 커서가_없으면_null을_반환한다() {
      // when
      BooleanExpression seek = keysetSeek(ReviewSortType.POPULAR, SortOrder.DESC, null);

      // then
      assertThat(seek).isNull();
    }

    @Test
    @DisplayName("POPULAR DESC에서 좋아요만 보던 기존 버그가 고쳐져 isBest가 다른 행은 좋아요 수와 무관하게 다음 페이지에 포함된다")
    void popular_desc에서_isBest가_다른_행은_좋아요와_무관하게_다음_페이지에_포함된다() {
      // given: 1페이지 마지막 행이 best(likes=2)였던 상황의 커서.
      // 데이터: best(likes=3), best(likes=2), best(likes=1), notBest(likes=100) 이 이 순서로 정렬된다.
      ReviewInfo lastOfPage1 = reviewInfo(2L, true, 2L, null, null, null);
      CursorClaims claims = claimsOf(cursorKeys(ReviewSortType.POPULAR, lastOfPage1));

      // when
      BooleanExpression actual = keysetSeek(ReviewSortType.POPULAR, SortOrder.DESC, claims);

      // then: sortBy의 POPULAR 구조(isBest desc nullsLast -> likes desc nullsLast -> createAt desc ->
      // id desc)를
      // 그대로 튜플 비교로 옮긴 것과 동일해야 한다.
      BooleanExpression tail =
          review.createAt.lt(CREATE_AT).or(review.createAt.eq(CREATE_AT).and(review.id.lt(2L)));
      NumberExpression<Long> likesCount = likes.id.countDistinct();
      BooleanExpression likesStep =
          likesCount.lt(2L).or(likesCount.isNull()).or(likesCount.eq(2L).and(tail));
      BooleanExpression expected =
          review
              .isBest
              .eq(false)
              .or(review.isBest.isNull())
              .or(review.isBest.eq(true).and(likesStep));
      assertThat(actual.toString()).isEqualTo(expected.toString());

      // isBest = false 조건이 likes 비교와 무관하게 최상위 OR로 걸려 있으므로
      // notBest(likes=100)처럼 likes가 커서 값보다 큰 행도 반드시 포함된다.
      assertThat(actual.toString()).contains("review.isBest = false || review.isBest is null");
    }

    @Test
    @DisplayName("LIKES 정렬은 좋아요 수 -> createAt -> id 순으로만 seek한다")
    void likes_정렬은_좋아요_수_기준으로만_seek한다() {
      // given
      ReviewInfo lastOfPage1 = reviewInfo(9L, null, 5L, null, null, null);
      CursorClaims claims = claimsOf(cursorKeys(ReviewSortType.LIKES, lastOfPage1));

      // when
      BooleanExpression actual = keysetSeek(ReviewSortType.LIKES, SortOrder.ASC, claims);

      // then
      BooleanExpression tail =
          review.createAt.lt(CREATE_AT).or(review.createAt.eq(CREATE_AT).and(review.id.lt(9L)));
      NumberExpression<Long> likesCount = likes.id.countDistinct();
      BooleanExpression expected = likesCount.gt(5L).or(likesCount.eq(5L).and(tail));
      assertThat(actual.toString()).isEqualTo(expected.toString());
    }

    @Test
    @DisplayName("RATING ASC에서 커서 값이 NULL이면 MySQL의 nulls-first 규칙대로 NOT NULL 행이 모두 앞선 것으로 본다")
    void rating_asc에서_커서가_null이면_not_null_행을_모두_이후로_본다() {
      // given: reviewRating이 없는 행이 1페이지 마지막이었던 상황
      ReviewInfo lastOfPage1 = reviewInfo(5L, null, null, null, null, null);
      CursorClaims claims = claimsOf(cursorKeys(ReviewSortType.RATING, lastOfPage1));

      // when
      BooleanExpression actual = keysetSeek(ReviewSortType.RATING, SortOrder.ASC, claims);

      // then: ASC는 MySQL 기본 정렬상 NULL이 맨 앞이므로, NULL 아닌 행은 전부 이후 페이지에 포함돼야 한다.
      BooleanExpression tail =
          review.createAt.lt(CREATE_AT).or(review.createAt.eq(CREATE_AT).and(review.id.lt(5L)));
      BooleanExpression expected =
          review.reviewRating.isNotNull().or(review.reviewRating.isNull().and(tail));
      assertThat(actual.toString()).isEqualTo(expected.toString());
    }

    @Test
    @DisplayName("RATING DESC는 리뷰 별점 컬럼으로 seek한다")
    void rating_desc는_리뷰_별점_컬럼으로_seek한다() {
      // given
      ReviewInfo lastOfPage1 = reviewInfo(5L, null, null, 4.5, null, null);
      CursorClaims claims = claimsOf(cursorKeys(ReviewSortType.RATING, lastOfPage1));

      // when
      BooleanExpression actual = keysetSeek(ReviewSortType.RATING, SortOrder.DESC, claims);

      // then
      BooleanExpression tail =
          review.createAt.lt(CREATE_AT).or(review.createAt.eq(CREATE_AT).and(review.id.lt(5L)));
      BooleanExpression expected =
          review
              .reviewRating
              .lt(4.5)
              .or(review.reviewRating.isNull())
              .or(review.reviewRating.eq(4.5).and(tail));
      assertThat(actual.toString()).isEqualTo(expected.toString());
    }

    @DisplayName("RATING DESC에서 커서 값이 NULL이면 이미 NULL 꼬리 구간이므로 id로만 seek한다")
    void rating_desc에서_커서가_null이면_null_꼬리_구간을_id로만_seek한다() {
      // given
      ReviewInfo lastOfPage1 = reviewInfo(5L, null, null, null, null, null);
      CursorClaims claims = claimsOf(cursorKeys(ReviewSortType.RATING, lastOfPage1));

      // when
      BooleanExpression actual = keysetSeek(ReviewSortType.RATING, SortOrder.DESC, claims);

      // then: DESC는 MySQL 기본 정렬상 NULL이 맨 뒤이므로, 이후 행도 NULL이면서 id만 더 작아야 한다.
      BooleanExpression tail =
          review.createAt.lt(CREATE_AT).or(review.createAt.eq(CREATE_AT).and(review.id.lt(5L)));
      BooleanExpression expected = review.reviewRating.isNull().and(tail);
      assertThat(actual.toString()).isEqualTo(expected.toString());
    }

    @Test
    @DisplayName(
        "BOTTLE_PRICE는 sizeType(BOTTLE 우선, nullsLast) -> price -> createAt -> id 순으로 seek한다")
    void bottle_price는_sizeType을_먼저_확인한_뒤_price로_seek한다() {
      // given
      ReviewInfo lastOfPage1 =
          reviewInfo(7L, null, null, null, SizeType.BOTTLE, new BigDecimal("10000"));
      CursorClaims claims = claimsOf(cursorKeys(ReviewSortType.BOTTLE_PRICE, lastOfPage1));

      // when
      BooleanExpression actual = keysetSeek(ReviewSortType.BOTTLE_PRICE, SortOrder.ASC, claims);

      // then
      BooleanExpression tail =
          review.createAt.lt(CREATE_AT).or(review.createAt.eq(CREATE_AT).and(review.id.lt(7L)));
      BooleanExpression priceStep =
          review
              .price
              .gt(new BigDecimal("10000"))
              .or(review.price.eq(new BigDecimal("10000")).and(tail));
      BooleanExpression expected =
          review
              .sizeType
              .eq(SizeType.GLASS)
              .or(review.sizeType.isNull())
              .or(review.sizeType.eq(SizeType.BOTTLE).and(priceStep));
      assertThat(actual.toString()).isEqualTo(expected.toString());
    }

    @Test
    @DisplayName("GLASS_PRICE는 sizeType(GLASS 우선, nullsLast) -> price -> createAt -> id 순으로 seek한다")
    void glass_price는_sizeType을_먼저_확인한_뒤_price로_seek한다() {
      // given
      ReviewInfo lastOfPage1 =
          reviewInfo(8L, null, null, null, SizeType.GLASS, new BigDecimal("5000"));
      CursorClaims claims = claimsOf(cursorKeys(ReviewSortType.GLASS_PRICE, lastOfPage1));

      // when
      BooleanExpression actual = keysetSeek(ReviewSortType.GLASS_PRICE, SortOrder.DESC, claims);

      // then
      BooleanExpression tail =
          review.createAt.lt(CREATE_AT).or(review.createAt.eq(CREATE_AT).and(review.id.lt(8L)));
      BooleanExpression priceStep =
          review
              .price
              .lt(new BigDecimal("5000"))
              .or(review.price.eq(new BigDecimal("5000")).and(tail));
      BooleanExpression expected =
          review
              .sizeType
              .eq(SizeType.BOTTLE)
              .or(review.sizeType.isNull())
              .or(review.sizeType.eq(SizeType.GLASS).and(priceStep));
      assertThat(actual.toString()).isEqualTo(expected.toString());
    }
  }
}
