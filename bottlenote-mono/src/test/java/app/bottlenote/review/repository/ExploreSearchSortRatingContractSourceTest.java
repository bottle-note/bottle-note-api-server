package app.bottlenote.review.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** #414/#415/#417의 request-to-query 계약이 수직 경로에 남아 있는지 검증한다. */
@Tag("unit")
@SuppressWarnings("NonAsciiCharacters")
class ExploreSearchSortRatingContractSourceTest {

  @Test
  @DisplayName("리뷰 둘러보기는 신규 keyword·정렬·rating을 criteria와 keyset 쿼리까지 전달한다")
  void review_explore_contract_is_wired_end_to_end() throws IOException {
    String request = read("bottlenote-mono/src/main/java/app/bottlenote/review/dto/request/ReviewExploreRequest.java");
    String criteria = read("bottlenote-mono/src/main/java/app/bottlenote/review/dto/dsl/ReviewExploreCriteria.java");
    String repository = read("bottlenote-mono/src/main/java/app/bottlenote/review/repository/CustomReviewRepositoryImpl.java");

    assertThat(request)
        .contains(
            "String keyword",
            "List<String> keywords",
            "ReviewSortType sortType",
            "SortOrder sortOrder",
            "BigDecimal ratingFrom",
            "BigDecimal ratingTo")
        .contains("ReviewSortType.LATEST", "EXPLORE_KEYWORD_CONFLICT", "EXPLORE_RATING_INVALID");
    assertThat(criteria)
        .contains("effectiveKeywords", "sortType", "sortOrder", "ratingFrom", "ratingTo");
    assertThat(repository)
        .contains(
            "criteria.effectiveKeywords()",
            "criteria.ratingFrom()",
            "criteria.ratingTo()",
            "criteria.sortType()",
            "criteria.sortOrder()")
        .contains("keysetSeek(criteria.sortType(), criteria.sortOrder()")
        .doesNotContain(".orderBy(review.createAt.desc(), review.id.desc())");
  }

  @Test
  @DisplayName("위스키 둘러보기 rating은 목록 표시와 같은 반올림 집계값으로 후보 단계에서 필터링한다")
  void alcohol_explore_rating_uses_displayed_aggregate() throws IOException {
    String request = read("bottlenote-mono/src/main/java/app/bottlenote/alcohols/dto/request/ExploreStandardRequest.java");
    String criteria = read("bottlenote-mono/src/main/java/app/bottlenote/alcohols/dto/dsl/ExploreStandardCriteria.java");
    String repository = read("bottlenote-mono/src/main/java/app/bottlenote/alcohols/repository/CustomAlcoholQueryRepositoryImpl.java");

    assertThat(request)
        .contains("BigDecimal ratingFrom", "BigDecimal ratingTo", "EXPLORE_RATING_INVALID");
    assertThat(criteria)
        .contains("BigDecimal ratingFrom", "BigDecimal ratingTo", "request.ratingFrom()", "request.ratingTo()");
    assertThat(repository)
        .contains("displayedRating()", "ratingInRange(BigDecimal from, BigDecimal to)")
        .contains(".having(", "ratingInRange(criteria.ratingFrom(), criteria.ratingTo())");

    String randomBranch = extractRandomBranch(repository);
    assertThat(randomBranch)
        .contains("if (criteria.hasRatingRange())")
        .contains("leftJoin(rating)", "ratingInRange(criteria.ratingFrom(), criteria.ratingTo())")
        .contains("groupBy(alcohol.id)", "randomSeek(claims, crc)");
  }

  private static String extractRandomBranch(String source) {
    int start = source.indexOf("if (sortType == SearchSortType.RANDOM)");
    int end = source.indexOf("NumberExpression<? extends Number> sortScore", start);
    assertThat(start).isGreaterThanOrEqualTo(0);
    assertThat(end).isGreaterThan(start);
    return source.substring(start, end);
  }

  private static String read(String relativePath) throws IOException {
    Path path = Paths.get(relativePath);
    if (!Files.exists(path)) {
      path = Paths.get(relativePath.replaceFirst("^bottlenote-mono/", ""));
    }
    return Files.readString(path);
  }
}
