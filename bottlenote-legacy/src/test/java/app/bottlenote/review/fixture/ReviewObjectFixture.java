package app.bottlenote.review.fixture;

import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewLocation;
import app.bottlenote.shared.alcohols.payload.AlcoholSummaryItem;
import app.bottlenote.shared.cursor.CursorPageable;
import app.bottlenote.shared.cursor.PageResponse;
import app.bottlenote.shared.review.constant.ReviewDisplayStatus;
import app.bottlenote.shared.review.constant.SizeType;
import app.bottlenote.shared.review.dto.request.LocationInfoRequest;
import app.bottlenote.shared.review.dto.request.ReviewCreateRequest;
import app.bottlenote.shared.review.dto.request.ReviewImageInfoRequest;
import app.bottlenote.shared.review.dto.request.ReviewModifyRequest;
import app.bottlenote.shared.review.dto.response.ReviewCreateResponse;
import app.bottlenote.shared.review.dto.response.ReviewDetailResponse;
import app.bottlenote.shared.review.dto.response.ReviewListResponse;
import app.bottlenote.shared.review.payload.ReviewInfo;
import app.bottlenote.shared.review.payload.UserInfo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

public class ReviewObjectFixture {

  /** 기본 ReviewInfo 객체를 생성합니다. */
  public static ReviewInfo getReviewInfo() {
    return getReviewInfo(1L, "이것은 샘플 리뷰입니다.", BigDecimal.valueOf(10000L), SizeType.GLASS);
  }

  /** 지정된 매개변수로 ReviewInfo 객체를 생성합니다. */
  public static ReviewInfo getReviewInfo(
      Long reviewId, String reviewContent, BigDecimal price, SizeType sizeType) {
    return ReviewInfo.builder()
        .reviewId(reviewId)
        .reviewContent(reviewContent)
        .price(price)
        .sizeType(sizeType)
        .likeCount(10L)
        .replyCount(2L)
        .reviewImageUrl("https://example.com/review-image.jpg")
        .userInfo(getRandomUserInfo())
        .rating(4.5)
        .viewCount(0L)
        .locationInfo(getRandomLocationInfo().toLocationInfo())
        .status(ReviewDisplayStatus.PUBLIC)
        .isMyReview(true)
        .isLikedByMe(false)
        .hasReplyByMe(true)
        .isBestReview(false)
        .tastingTagList("과일향,부드러움")
        .createAt(LocalDateTime.now())
        .build();
  }

  private static UserInfo getRandomUserInfo() {
    return new UserInfo(1L, "홍길동", "https://example.com/profile.jpg");
  }

  private static ReviewLocation getRandomLocationInfo() {
    return ReviewLocation.builder()
        .name("도시술")
        .address("서울 송파구 송파대로 145")
        .detailAddress("2층 도시술")
        .category("음식점 > 술집 > 칵테일바")
        .mapUrl("https://place.map.kakao.com/2088591613")
        .latitude("37.4835934678036")
        .longitude("127.122831408454")
        .build();
  }

  public static ReviewListResponse getReviewListResponse(int size) {
    List<ReviewInfo> reviewInfo = new ArrayList<>();

    for (int i = 0; i < size; i++) {
      reviewInfo.add(
          getReviewInfo(
              (long) i,
              "좋은 리뷰입니다 (" + RandomStringUtils.randomAlphabetic(10) + ")",
              BigDecimal.valueOf(100000L),
              SizeType.BOTTLE));
    }

    return ReviewListResponse.of((long) reviewInfo.size(), reviewInfo);
  }

  public static PageResponse<ReviewListResponse> getReviewListResponse() {
    ReviewInfo reviewResponse1 =
        getReviewInfo(1L, "맛있어요", BigDecimal.valueOf(100000L), SizeType.BOTTLE);
    ReviewInfo reviewResponse2 =
        getReviewInfo(2L, "나름 먹을만 하네요", BigDecimal.valueOf(110000L), SizeType.BOTTLE);
    List<ReviewInfo> reviewResponse = List.of(reviewResponse1, reviewResponse2);
    CursorPageable cursorPageable =
        CursorPageable.builder().currentCursor(0L).cursor(1L).pageSize(2L).hasNext(false).build();
    ReviewListResponse response =
        ReviewListResponse.of((long) reviewResponse.size(), reviewResponse);
    return PageResponse.of(response, cursorPageable);
  }

  public static ReviewDetailResponse getReviewDetailResponse() {
    return ReviewDetailResponse.create(
        getAlcoholInfo(),
        getReviewInfo(),
        List.of(
            new ReviewImageInfoRequest(
                1L, "https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/1")));
  }

  /** 기본 ReviewModifyRequestWrapperItem 객체를 생성합니다. */
  public static ReviewModifyRequest getReviewModifyRequest(ReviewDisplayStatus status) {
    return new ReviewModifyRequest(
        "그저 그래요",
        status,
        BigDecimal.valueOf(10000L),
        List.of(
            new ReviewImageInfoRequest(
                1L, "https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/1")),
        SizeType.GLASS,
        List.of(),
        new LocationInfoRequest(
            "xxPub",
            "12345",
            "서울시 강남구 청담동",
            "xx빌딩",
            "PUB",
            "https://map.naver.com",
            "111.111",
            "222.222"));
  }

  public static ReviewModifyRequest getReviewModifyRequest(String content) {
    return new ReviewModifyRequest(
        content,
        ReviewDisplayStatus.PUBLIC,
        BigDecimal.valueOf(10000L),
        List.of(
            new ReviewImageInfoRequest(
                1L, "https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/1")),
        SizeType.GLASS,
        List.of(),
        new LocationInfoRequest(
            "xxPub",
            "12345",
            "서울시 강남구 청담동",
            "xx빌딩",
            "PUB",
            "https://map.naver.com",
            "111.111",
            "222.222"));
  }

  public static ReviewModifyRequest getNullableReviewModifyRequest(ReviewDisplayStatus status) {
    return new ReviewModifyRequest("맛있어요", status, null, null, null, null, null);
  }

  public static ReviewModifyRequest getWrongReviewModifyRequest() {
    return new ReviewModifyRequest(null, null, null, null, null, null, null);
  }

  /** 기본 ReviewCreateRequest 객체를 생성합니다. */
  public static ReviewCreateRequest getReviewCreateRequest() {
    return new ReviewCreateRequest(
        1L,
        ReviewDisplayStatus.PUBLIC,
        "맛있어요",
        SizeType.GLASS,
        BigDecimal.valueOf(30000L),
        new LocationInfoRequest(
            "xxPub",
            "12345",
            "서울시 강남구 청담동",
            "xx빌딩",
            "PUB",
            "https://map.naver.com",
            "111.111",
            "222.222"),
        List.of(
            new ReviewImageInfoRequest(
                1L, "https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/1"),
            new ReviewImageInfoRequest(
                2L, "https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/2"),
            new ReviewImageInfoRequest(
                3L, "https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/3")),
        List.of("테이스팅태그1", "테이스팅태그2", "테이스팅태그3"),
        0.5);
  }

  public static ReviewCreateRequest getReviewCreateRequest(String content, BigDecimal price) {
    return new ReviewCreateRequest(
        1L,
        ReviewDisplayStatus.PUBLIC,
        content,
        SizeType.GLASS,
        price,
        new LocationInfoRequest(
            "xxPub",
            "12345",
            "서울시 강남구 청담동",
            "xx빌딩",
            "PUB",
            "https://map.naver.com",
            "111.111",
            "222.222"),
        List.of(
            new ReviewImageInfoRequest(
                1L, "https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/1")),
        List.of("테이스팅태그1"),
        0.5);
  }

  public static ReviewCreateResponse getReviewCreateResponse() {
    return ReviewCreateResponse.builder().id(1L).content("맛있어요").callback("1").build();
  }

  /** 기본 Review 도메인 객체를 생성합니다. */
  public static Review getReviewFixture() {
    return getReviewFixture(1L, 1L, "맛있어요");
  }

  public static Review getReviewFixture(Long alcoholId, Long userId, String content) {
    return Review.builder()
        .id(1L)
        .alcoholId(alcoholId)
        .userId(userId)
        .content(content)
        .reviewLocation(null)
        .build();
  }

  public static AlcoholSummaryItem getAlcoholInfo() {
    return new AlcoholSummaryItem(
        1L, "글래스고 12년산", "1770 글래스고 싱글 몰트", "싱글 몰트", "Single Malt", "ImageUrl", false);
  }
}
