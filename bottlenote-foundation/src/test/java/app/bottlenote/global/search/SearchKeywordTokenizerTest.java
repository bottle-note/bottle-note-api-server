package app.bottlenote.global.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("SearchKeywordTokenizer")
class SearchKeywordTokenizerTest {

  @Test
  @DisplayName("검색어를 소문자 토큰으로 분리하고 입력 순서대로 중복을 제거한다")
  void tokenize_normalizes_and_deduplicates() {
    assertThat(SearchKeywordTokenizer.tokenize("  PEAT   맥캘란 peat  "))
        .containsExactly("peat", "맥캘란");
  }

  @Test
  @DisplayName("null 또는 공백 검색어는 빈 토큰 목록을 반환한다")
  void tokenize_blank_returns_empty_tokens() {
    assertThat(SearchKeywordTokenizer.tokenize(null)).isEqualTo(List.of());
    assertThat(SearchKeywordTokenizer.tokenize("   ")).isEqualTo(List.of());
  }
}
