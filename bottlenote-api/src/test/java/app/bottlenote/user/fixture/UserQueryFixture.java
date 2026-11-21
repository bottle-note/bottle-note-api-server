package app.bottlenote.user.fixture;

import app.bottlenote.global.pagination.KeysetPageResponse;
import app.bottlenote.global.pagination.KeysetPagination;
import app.bottlenote.user.dto.response.MyBottleResponse;
import app.bottlenote.user.dto.response.MyPageResponse;
import app.bottlenote.user.dto.response.PicksMyBottleItem;
import app.bottlenote.user.dto.response.RatingMyBottleItem;
import app.bottlenote.user.dto.response.ReviewMyBottleItem;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class UserQueryFixture {

  public MyPageResponse getMyPageInfo() {

    return MyPageResponse.builder()
        .userId(1L)
        .nickName("nickname")
        .imageUrl("imageUrl")
        .reviewCount(10L)
        .ratingCount(20L)
        .pickCount(30L)
        .followerCount(40L)
        .followingCount(50L)
        .isFollow(false)
        .isMyPage(false)
        .build();
  }

  public KeysetPageResponse<MyBottleResponse> getReviewMyBottleResponse(
      Long userId, boolean isMyPage) {
    LocalDateTime now = LocalDateTime.now();

    ReviewMyBottleItem review1 =
        new ReviewMyBottleItem(
            new MyBottleResponse.BaseMyBottleInfo(
                1L,
                "글렌피딕 12년",
                "Glenfiddich 12 Year Old",
                "싱글 몰트 위스키",
                "https://example.com/image1.jpg",
                true),
            1L,
            true, // isMyReview
            now, // reviewModifyAt
            "부드럽고 향긋한 맛", // reviewContent
            Set.of("과일향", "바닐라"), // reviewFlavorTags
            true, // isBestReview
            now,
            4.0);

    ReviewMyBottleItem review2 =
        new ReviewMyBottleItem(
            new MyBottleResponse.BaseMyBottleInfo(
                2L,
                "맥캘란 18년",
                "Macallan 18 Year Old",
                "싱글 몰트 위스키",
                "https://example.com/image2.jpg",
                false),
            1L,
            false,
            now,
            "깊고 진한 풍미",
            Set.of("스모키", "오크"),
            false,
            now,
            3.0);

    List<ReviewMyBottleItem> reviewList = List.of(review1, review2);

    MyBottleResponse myBottleResponse = MyBottleResponse.create(userId, isMyPage, reviewList);
    return KeysetPageResponse.of(myBottleResponse, new KeysetPagination(false, null));
  }

  public KeysetPageResponse<MyBottleResponse> getRatingMyBottleResponse(
      Long userId, boolean isMyPage) {

    RatingMyBottleItem ratingMyBottleItem1 =
        RatingMyBottleItem.builder()
            .baseMyBottleInfo(
                new MyBottleResponse.BaseMyBottleInfo(
                    1L,
                    "글렌피딕 12년",
                    "Glenfiddich 12 Year Old",
                    "싱글 몰트 위스키",
                    "https://example.com/image1.jpg",
                    true))
            .myRatingPoint(1.0)
            .averageRatingCount(3L)
            .averageRatingPoint(3.0)
            .ratingModifyAt(LocalDateTime.now())
            .lastReviewAt(LocalDateTime.now())
            .build();

    List<RatingMyBottleItem> ratingList = List.of(ratingMyBottleItem1);

    MyBottleResponse myBottleResponse = MyBottleResponse.create(userId, isMyPage, ratingList);
    return KeysetPageResponse.of(myBottleResponse, new KeysetPagination(false, null));
  }

  public KeysetPageResponse<MyBottleResponse> getPicksMyBottleResponse(
      Long userId, boolean isMyPage) {

    PicksMyBottleItem picksMyBottleItem1 =
        PicksMyBottleItem.builder()
            .baseMyBottleInfo(
                new MyBottleResponse.BaseMyBottleInfo(
                    1L,
                    "글렌피딕 12년",
                    "Glenfiddich 12 Year Old",
                    "싱글 몰트 위스키",
                    "https://example.com/image1.jpg",
                    true))
            .isPicked(true)
            .totalPicksCount(100L)
            .lastModifyAt(LocalDateTime.now())
            .lastReviewAt(LocalDateTime.now())
            .myRatingPoint(4.0)
            .build();

    List<PicksMyBottleItem> picksList = List.of(picksMyBottleItem1);

    MyBottleResponse myBottleResponse = MyBottleResponse.create(userId, isMyPage, picksList);
    return KeysetPageResponse.of(myBottleResponse, new KeysetPagination(false, null));
  }
}
