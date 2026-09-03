package app.bottlenote.review.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** #414/#415/#417/#430의 request-to-query 계약이 수직 경로에 남아 있는지 검증한다. */
@Tag("unit")
@SuppressWarnings("NonAsciiCharacters")
class ExploreSearchSortRatingContractSourceTest {

  @Test
  @DisplayName("리뷰 둘러보기는 신규 keyword·정렬·rating을 criteria와 keyset 쿼리까지 전달한다")
  void review_explore_contract_is_wired_end_to_end() throws IOException {
    String request =
        read(
            "bottlenote-mono/src/main/java/app/bottlenote/review/dto/request/ReviewExploreRequest.java");
    String criteria =
        read(
            "bottlenote-mono/src/main/java/app/bottlenote/review/dto/dsl/ReviewExploreCriteria.java");
    String repository =
        read(
            "bottlenote-mono/src/main/java/app/bottlenote/review/repository/CustomReviewRepositoryImpl.java");

    assertThat(request)
        .contains(
            "String keyword",
            "ReviewSortType sortType",
            "SortOrder sortOrder",
            "BigDecimal ratingFrom",
            "BigDecimal ratingTo")
        .contains("ReviewSortType.LATEST", "EXPLORE_RATING_INVALID")
        .doesNotContain("List<String> keywords", "EXPLORE_KEYWORD_CONFLICT");
    assertThat(criteria)
        .contains("searchTokens", "sortType", "sortOrder", "ratingFrom", "ratingTo")
        .doesNotContain("effectiveKeywords", "List<String> keywords");
    assertThat(repository)
        .contains(
            "criteria.searchTokens()",
            "criteria.ratingFrom()",
            "criteria.ratingTo()",
            "criteria.sortType()",
            "criteria.sortOrder()")
        .contains("keysetSeek(criteria.sortType(), criteria.sortOrder()")
        .doesNotContain(".orderBy(review.createAt.desc(), review.id.desc())");
  }

  @Test
  @DisplayName("위스키 둘러보기 rating 필터는 기존 0.5 단위 집계값을 유지한다")
  void alcohol_explore_rating_filter_keeps_half_step_aggregate() throws IOException {
    String request =
        read(
            "bottlenote-mono/src/main/java/app/bottlenote/alcohols/dto/request/ExploreStandardRequest.java");
    String criteria =
        read(
            "bottlenote-mono/src/main/java/app/bottlenote/alcohols/dto/dsl/ExploreStandardCriteria.java");
    String repository =
        read(
            "bottlenote-mono/src/main/java/app/bottlenote/alcohols/repository/CustomAlcoholQueryRepositoryImpl.java");

    assertThat(request)
        .contains(
            "String keyword",
            "BigDecimal ratingFrom",
            "BigDecimal ratingTo",
            "EXPLORE_RATING_INVALID")
        .doesNotContain("List<String> keywords");
    assertThat(criteria)
        .contains(
            "List<String> searchTokens",
            "BigDecimal ratingFrom",
            "BigDecimal ratingTo",
            "request.ratingFrom()",
            "request.ratingTo()");
    assertThat(repository)
        .contains("filterRating()", "ratingInRange(BigDecimal from, BigDecimal to)")
        .contains(".having(", "ratingInRange(criteria.ratingFrom(), criteria.ratingTo())");

    String randomBranch = extractRandomBranch(repository);
    assertThat(randomBranch)
        .contains("if (criteria.hasRatingRange())")
        .contains("leftJoin(rating)", "ratingInRange(criteria.ratingFrom(), criteria.ratingTo())")
        .contains("groupBy(alcohol.id)", "randomSeek(claims, crc)");
  }

  @Test
  @DisplayName("알코올 별점은 0.1 단위로 노출하고 사용자 리뷰 별점은 최신 활성 단건을 조회한다")
  void alcohol_rating_display_uses_tenth_and_single_user_review() throws IOException {
    String repository =
        read(
            "bottlenote-mono/src/main/java/app/bottlenote/alcohols/repository/CustomAlcoholQueryRepositoryImpl.java");
    String supporter =
        read(
            "bottlenote-mono/src/main/java/app/bottlenote/alcohols/repository/AlcoholQuerySupporter.java");
    String popularRepository =
        read(
            "bottlenote-mono/src/main/java/app/bottlenote/alcohols/repository/CustomPopularQueryRepositoryImpl.java");
    String historyRepository =
        read(
            "bottlenote-mono/src/main/java/app/bottlenote/history/repository/JpaAlcoholsViewHistoryRepository.java");
    String ratingSupporter =
        read(
            "bottlenote-mono/src/main/java/app/bottlenote/rating/repository/RatingQuerySupporter.java");

    String displayedRating =
        extractMethodBodyBySignature(
            supporter, "public static NumberExpression<Double> displayedRating()");
    String filterRating = extractMethodBody(repository, "filterRating");
    String longUserReviewRating =
        extractMethodBodyBySignature(
            supporter, "latestActiveUserReviewRating(Long alcoholId, Long userId)");
    String pathUserReviewRating =
        extractMethodBodyByNthSignature(supporter, "latestActiveUserReviewRating(", 2);
    String detailItem =
        read(
            "bottlenote-mono/src/main/java/app/bottlenote/alcohols/dto/response/AlcoholDetailItem.java");
    String rawRatingSort = extractMethodBody(supporter, "sortBy");
    String rawRatingCursor = extractMethodBody(supporter, "sortScore");
    assertThat(displayedRating)
        .contains(".avg()", ".multiply(10)", ".round()", ".divide(10)", ".coalesce(0.0)")
        .doesNotContain(".multiply(2)", ".divide(2)");
    assertThat(filterRating)
        .contains(".avg()", ".multiply(2)", ".round()", ".divide(2)", ".coalesce(0.0)");
    for (String userReviewRating : List.of(longUserReviewRating, pathUserReviewRating)) {
      assertThat(userReviewRating)
          .contains(
              "if (userId == null || userId == -1L)",
              "review.reviewRating",
              "review.id.eq(",
              "JPAExpressions.select(latestReview.id.max())",
              "latestReview.alcoholId.eq(alcoholId)",
              "latestReview.userId.eq(userId)",
              "latestReview.activeStatus.eq(ACTIVE)",
              ".coalesce(0.0)",
              ".as(\"myAvgRating\")")
          .doesNotContain(".avg()", ".orderBy(review.id.desc())", ".limit(1)");
    }
    assertThat(detailItem)
        .contains(
            "@JsonPropertyDescription(\"인증 사용자가 해당 알코올에 남긴 ACTIVE 리뷰 중 최신(id 최대) 1건의 별점, 없으면 0.0\")",
            "private Double myAvgRating");
    assertThat(supporter).doesNotContain("userReviewRating(");
    for (String rawRatingExpression : List.of(rawRatingSort, rawRatingCursor)) {
      assertThat(rawRatingExpression)
          .contains("rating.ratingPoint.rating.avg().coalesce(0.0)")
          .doesNotContain("displayedRating()", ".multiply(10)", ".round()", ".divide(10)");
    }
    assertThat(repository)
        .contains("displayedRating().as(\"rating\")")
        .doesNotContain("private static NumberExpression<Double> displayedRating()");
    assertThat(popularRepository)
        .contains("displayedRating(),", ".orderBy(snapshotScore.desc(), alcohol.id.asc())")
        .doesNotContain("private NumberExpression<Double> displayedRating()")
        .doesNotContain(".orderBy(displayedRating()");
    assertThat(historyRepository)
        .contains(
            "CAST(ROUND((SELECT COALESCE(AVG(r.ratingPoint.rating), 0.0)",
            "a.id.alcoholId), 1) AS double)");
    String averageRatingSubQuery =
        extractMethodBodyBySignature(
            ratingSupporter, "averageRatingSubQuery(NumberPath<Long> alocholId)");
    assertThat(averageRatingSubQuery)
        .contains(".avg()", ".multiply(10)", ".round()", ".divide(10)", ".coalesce(0.0)")
        .doesNotContain("avg().round()");
  }

  private static String extractRandomBranch(String source) {
    int start = source.indexOf("if (sortType == SearchSortType.RANDOM)");
    int end = source.indexOf("NumberExpression<? extends Number> sortScore", start);
    assertThat(start).isGreaterThanOrEqualTo(0);
    assertThat(end).isGreaterThan(start);
    return source.substring(start, end);
  }

  private static String extractMethodBody(String source, String methodName) {
    return extractMethodBodyBySignature(source, " " + methodName + "(");
  }

  private static String extractMethodBodyByNthSignature(
      String source, String signature, int occurrence) {
    int nameIndex = -1;
    for (int count = 0; count < occurrence; count++) {
      nameIndex = source.indexOf(signature, nameIndex + 1);
    }
    return extractMethodBodyFromIndex(source, signature, nameIndex);
  }

  private static String extractMethodBodyBySignature(String source, String signature) {
    return extractMethodBodyFromIndex(source, signature, source.indexOf(signature));
  }

  private static String extractMethodBodyFromIndex(String source, String signature, int nameIndex) {
    assertThat(nameIndex).as("method not found: " + signature).isGreaterThanOrEqualTo(0);
    int braceStart = source.indexOf('{', nameIndex);
    int depth = 0;
    for (int index = braceStart; index < source.length(); index++) {
      char character = source.charAt(index);
      if (character == '{') {
        depth++;
      } else if (character == '}' && --depth == 0) {
        return source.substring(braceStart, index + 1);
      }
    }
    throw new IllegalStateException("method body end not found: " + signature);
  }

  private static String read(String relativePath) throws IOException {
    Path path = Paths.get(relativePath);
    if (!Files.exists(path)) {
      path = Paths.get(relativePath.replaceFirst("^bottlenote-mono/", ""));
    }
    return Files.readString(path);
  }
}
