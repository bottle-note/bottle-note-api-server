package app.bottlenote.global.search;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.alcohols.dto.dsl.ExploreStandardCriteria;
import app.bottlenote.alcohols.dto.request.ExploreStandardRequest;
import app.bottlenote.review.dto.dsl.ReviewExploreCriteria;
import app.bottlenote.review.dto.request.ReviewExploreRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("둘러보기 keyword cursor context")
class ExploreKeywordCursorContextTest {

  @Test
  @DisplayName("리뷰 cursor context는 정규화 토큰이 같으면 같고 토큰이 다르면 다르다")
  void review_context_uses_normalized_search_tokens() {
    String normalized = reviewContext("  PEAT   peat  ");

    assertThat(normalized).isEqualTo(reviewContext("peat"));
    assertThat(normalized).isNotEqualTo(reviewContext("smoke"));
  }

  @Test
  @DisplayName("위스키 cursor context는 정규화 토큰이 같으면 같고 토큰이 다르면 다르다")
  void alcohol_context_uses_normalized_search_tokens() {
    String normalized = alcoholContext("  PEAT   peat  ");

    assertThat(normalized).isEqualTo(alcoholContext("peat"));
    assertThat(normalized).isNotEqualTo(alcoholContext("smoke"));
  }

  private String reviewContext(String keyword) {
    ReviewExploreRequest request = ReviewExploreRequest.builder().keyword(keyword).build();
    return ReviewExploreCriteria.of(request, -1L).context();
  }

  private String alcoholContext(String keyword) {
    ExploreStandardRequest request = ExploreStandardRequest.builder().keyword(keyword).build();
    return ExploreStandardCriteria.of(request, -1L, 0L, null).context();
  }
}
