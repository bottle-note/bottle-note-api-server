package app.bottlenote.alcohols.fixture;

import static app.bottlenote.alcohols.constant.AlcoholCategoryGroup.BLEND;
import static app.bottlenote.alcohols.constant.AlcoholCategoryGroup.BOURBON;
import static app.bottlenote.alcohols.constant.AlcoholCategoryGroup.OTHER;
import static app.bottlenote.alcohols.constant.AlcoholCategoryGroup.RYE;
import static app.bottlenote.alcohols.constant.AlcoholCategoryGroup.SINGLE_MALT;
import static app.bottlenote.review.constant.SizeType.GLASS;

import app.bottlenote.alcohols.constant.SearchSortType;
import app.bottlenote.alcohols.dto.request.AlcoholSearchRequest;
import app.bottlenote.alcohols.dto.response.AlcoholDetailItem;
import app.bottlenote.alcohols.dto.response.AlcoholSearchResponse;
import app.bottlenote.alcohols.dto.response.AlcoholsSearchItem;
import app.bottlenote.alcohols.dto.response.CategoryItem;
import app.bottlenote.alcohols.dto.response.FriendsDetailResponse;
import app.bottlenote.alcohols.dto.response.ReviewsDetailResponse;
import app.bottlenote.review.facade.payload.ReviewInfo;
import app.bottlenote.review.facade.payload.UserInfo;
import app.bottlenote.shared.cursor.CursorPageable;
import app.bottlenote.shared.cursor.PageResponse;
import app.bottlenote.shared.cursor.SortOrder;
import app.bottlenote.user.facade.payload.FriendItem;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class AlcoholQueryFixture {
  public static AlcoholDetailItem getAlcoholDetailInfo() {
    Random random = new Random();

    // 랜덤 알코올 ID (1~100 범위)
    long randomId = random.nextInt(100) + 1;

    // 랜덤 위스키 이름 생성용 배열
    String[] distilleryNames = {"글렌피딕", "글렌리벳", "맥캘란", "글래스고", "야마자키", "발베니", "하이랜드 파크", "라프로익"};
    String[] whiskySuffixes = {"싱글몰트", "블렌디드", "리저브", "시그니처", "레어 캐스크", "더블 캐스크"};

    // 랜덤 위스키 이름 생성
    String randomKorDistillery = distilleryNames[random.nextInt(distilleryNames.length)];
    String randomEngDistillery = toEnglishName(randomKorDistillery);
    String randomKorSuffix = whiskySuffixes[random.nextInt(whiskySuffixes.length)];
    String randomEngSuffix = toEnglishSuffix(randomKorSuffix);

    // 랜덤 숙성 년수 (10~30년)
    int ageYear = random.nextInt(21) + 10;

    // 위스키 이름 구성
    String korName = randomKorDistillery + " " + ageYear + "년 " + randomKorSuffix + " 위스키";
    String engName = randomEngDistillery + " " + ageYear + " Years " + randomEngSuffix;

    // 랜덤 ABV (40~60% 범위)
    String abv = String.valueOf(40 + random.nextInt(21));

    // 랜덤 캐스크 타입
    String[] caskTypes = {
      "Ex-Bourbon Casks",
      "Sherry Casks",
      "Marriage of Ex-Bourbon & Virgin Oak Casks",
      "Port Wine Casks",
      "First-Fill Oloroso Sherry Butts",
      "Pedro Ximenez Sherry Casks"
    };
    String randomCask = caskTypes[random.nextInt(caskTypes.length)];

    // 랜덤 평가 수 (1~1000)
    long totalRatings = random.nextInt(1000) + 1;

    // 랜덤 내 평점 (0.5~5.0, 0.5 단위)
    double myRating = (Math.round(random.nextDouble() * 10) / 2.0);
    myRating = Math.max(myRating, 0.5);

    // 랜덤 내 평균 평점 (0.5~5.0, 0.5 단위)
    double myAvgRating = (Math.round(random.nextDouble() * 10) / 2.0);
    myAvgRating = Math.max(myAvgRating, 0.5);

    // 랜덤 픽 여부
    boolean isPicked = random.nextBoolean();

    // 랜덤 테이스팅 태그
    String[] tastingTags = {
      "달달한", "부드러운", "향긋한", "견과류", "후추향의", "스모키한",
      "과일향", "꿀향", "시트러스", "오크향", "바닐라", "카라멜",
      "초콜릿", "스파이시", "드라이", "플로럴", "피트향"
    };

    // 3~6개의 랜덤 태그 선택
    int tagCount = random.nextInt(4) + 3;
    List<String> selectedTags = new ArrayList<>();
    for (int i = 0; i < tagCount; i++) {
      String tag = tastingTags[random.nextInt(tastingTags.length)];
      if (!selectedTags.contains(tag)) {
        selectedTags.add(tag);
      }
    }
    String alcoholsTastingTags = String.join(",", selectedTags);

    return AlcoholDetailItem.builder()
        .alcoholId(randomId)
        .alcoholUrlImg(
            "https://static.whiskybase.com/storage/whiskies/"
                + randomId
                + "/"
                + (randomId + 404500)
                + "-big.jpg")
        .korName(korName)
        .engName(engName)
        .korCategory("싱글 몰트")
        .engCategory("Single Malt")
        .korRegion("스코틀랜드/하이랜드")
        .engRegion("Scotland/Highlands")
        .cask(randomCask)
        .abv(abv)
        .korDistillery(randomKorDistillery + " 디스틸러리")
        .engDistillery(randomEngDistillery + " Distillery")
        .rating(3.5) // rating은 요청대로 그대로 유지
        .totalRatingsCount(totalRatings)
        .myRating(myRating)
        .myAvgRating(myAvgRating)
        .isPicked(isPicked)
        .alcoholsTastingTags(alcoholsTastingTags)
        .build();
  }

  private static String toEnglishName(String korName) {
    Map<String, String> nameMap =
        Map.of(
            "글렌피딕", "Glenfiddich",
            "글렌리벳", "Glenlivet",
            "맥캘란", "Macallan",
            "글래스고", "Glasgow",
            "야마자키", "Yamazaki",
            "발베니", "Balvenie",
            "하이랜드 파크", "Highland Park",
            "라프로익", "Laphroaig");
    return nameMap.getOrDefault(korName, "Unknown");
  }

  private static String toEnglishSuffix(String korSuffix) {
    Map<String, String> suffixMap =
        Map.of(
            "싱글몰트", "Single Malt",
            "블렌디드", "Blended",
            "리저브", "Reserve",
            "시그니처", "Signature",
            "레어 캐스크", "Rare Cask",
            "더블 캐스크", "Double Cask");
    return suffixMap.getOrDefault(korSuffix, "");
  }

  public static FriendsDetailResponse getFriendsDetailInfo() {
    return FriendsDetailResponse.of(
        6L,
        List.of(
            FriendItem.of("https://picsum.photos/600/600", 1L, "늙은코끼리", 4.5),
            FriendItem.of("https://picsum.photos/600/600", 2L, "나무사자", 1.5),
            FriendItem.of("https://picsum.photos/600/600", 3L, "피자파인애플", 3.0),
            FriendItem.of("https://picsum.photos/600/600", 4L, "멘토스", 0.5),
            FriendItem.of("https://picsum.photos/600/600", 5L, "민트맛치토스", 5.0),
            FriendItem.of("https://picsum.photos/600/600", 6L, "목데이터", 1.0)));
  }

  public static ReviewsDetailResponse getReviewsDetailInfo() {
    List<ReviewInfo> bestReview =
        List.of(
            ReviewInfo.builder()
                .userInfo(new UserInfo(1L, null, "3342네임"))
                .reviewId(3L)
                .reviewContent(
                    "약간의 스파이시함과 오크의 향을 느낄 수 있는 위스키였어요. 하지만 피니시가 조금 짧은 느낌이었네요. 한 번쯤 시도해볼 만합니다.")
                .rating(5.0)
                .sizeType(GLASS)
                .price(BigDecimal.valueOf(150000.00))
                .viewCount(0L)
                .likeCount(0L)
                .isLikedByMe(false)
                .replyCount(2L)
                .hasReplyByMe(false)
                .status(null)
                .reviewImageUrl(null)
                .createAt(null)
                .build());

    List<ReviewInfo> recentReviewInfos =
        List.of(
            ReviewInfo.builder()
                .userInfo(new UserInfo(1L, null, "xcvx"))
                .reviewId(3L)
                .reviewContent(
                    "약간의 스파이시함과 오크의 향을 느낄 수 있는 위스키였어요. 하지만 피니시가 조금 짧은 느낌이었네요. 한 번쯤 시도해볼 만합니다.")
                .rating(5.0)
                .sizeType(GLASS)
                .price(BigDecimal.valueOf(150000.00))
                .viewCount(0L)
                .likeCount(0L)
                .isLikedByMe(false)
                .replyCount(2L)
                .hasReplyByMe(false)
                .status(null)
                .reviewImageUrl(null)
                .createAt(null)
                .build(),
            ReviewInfo.builder()
                .userInfo(new UserInfo(1L, null, "ghjnn"))
                .reviewId(5L)
                .reviewContent("맛있어요")
                .rating(4.5)
                .sizeType(null)
                .price(BigDecimal.valueOf(0.00))
                .viewCount(0L)
                .likeCount(0L)
                .isLikedByMe(false)
                .replyCount(0L)
                .hasReplyByMe(false)
                .status(null)
                .reviewImageUrl(null)
                .createAt(null)
                .build(),
            ReviewInfo.builder()
                .userInfo(new UserInfo(1L, null, "asaa"))
                .reviewId(7L)
                .reviewContent("이 위스키는 스파이시한 오크와 달콤한 과일 노트가 절묘하게 어우러져 있어요. 피니시는 길고 부드러워요.")
                .rating(4.5)
                .sizeType(null)
                .price(BigDecimal.valueOf(0.00))
                .viewCount(0L)
                .likeCount(0L)
                .isLikedByMe(false)
                .replyCount(0L)
                .hasReplyByMe(false)
                .status(null)
                .reviewImageUrl(null)
                .createAt(null)
                .build());

    return ReviewsDetailResponse.builder()
        .totalReviewCount(10L)
        .bestReviewInfos(bestReview)
        .recentReviewInfos(recentReviewInfos)
        .build();
  }

  public static AlcoholSearchRequest getRequest() {
    return AlcoholSearchRequest.builder()
        .keyword("glen")
        .category(SINGLE_MALT)
        .regionId(1L)
        .sortType(SearchSortType.REVIEW)
        .sortOrder(SortOrder.DESC)
        .cursor(0L)
        .pageSize(3L)
        .build();
  }

  public static PageResponse<AlcoholSearchResponse> getResponse() {

    AlcoholsSearchItem detail_1 =
        AlcoholsSearchItem.builder()
            .alcoholId(5L)
            .korName("아녹 24년")
            .engName("anCnoc 24-year-old")
            .korCategoryName("싱글 몰트")
            .engCategoryName("Single Malt")
            .imageUrl("https://static.whiskybase.com/storage/whiskies/6/6/989/270671-big.jpg")
            .rating(4.5)
            .ratingCount(1L)
            .reviewCount(0L)
            .pickCount(1L)
            .isPicked(false)
            .build();

    AlcoholsSearchItem detail_2 =
        AlcoholsSearchItem.builder()
            .alcoholId(1L)
            .korName("글래스고 1770 싱글몰트 스카치 위스키")
            .engName("1770 Glasgow Single Malt")
            .korCategoryName("싱글 몰트")
            .engCategoryName("Single Malt")
            .imageUrl("https://static.whiskybase.com/storage/whiskies/2/0/8916/404538-big.jpg")
            .rating(3.5)
            .ratingCount(3L)
            .reviewCount(1L)
            .pickCount(1L)
            .isPicked(true)
            .build();

    AlcoholsSearchItem detail_3 =
        AlcoholsSearchItem.builder()
            .alcoholId(2L)
            .korName("글래스고 1770 싱글몰트 스카치 위스키")
            .engName("1770 Glasgow Single Malt")
            .korCategoryName("싱글 몰트")
            .engCategoryName("Single Malt")
            .imageUrl("https://static.whiskybase.com/storage/whiskies/2/0/8888/404535-big.jpg")
            .rating(3.5)
            .ratingCount(1L)
            .reviewCount(0L)
            .pickCount(1L)
            .isPicked(true)
            .build();

    Long totalCount = 5L;
    List<AlcoholsSearchItem> details = List.of(detail_1, detail_2, detail_3);
    CursorPageable cursorPageable =
        CursorPageable.builder().currentCursor(0L).cursor(4L).pageSize(3L).hasNext(true).build();
    AlcoholSearchResponse response = AlcoholSearchResponse.of(totalCount, details);
    return PageResponse.of(response, cursorPageable);
  }

  public static List<CategoryItem> categoryResponses() {
    return List.of(
        new CategoryItem("SINGLE_MOLT", "싱글 몰트", SINGLE_MALT),
        new CategoryItem("BLENDED", "블렌디드", BLEND),
        new CategoryItem("BOURBON", "버번", BOURBON),
        new CategoryItem("RYE", "라이", RYE),
        new CategoryItem("OTHER", "기타", OTHER));
  }
}
