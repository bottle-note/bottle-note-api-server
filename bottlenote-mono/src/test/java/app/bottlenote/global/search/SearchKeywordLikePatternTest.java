package app.bottlenote.global.search;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("SearchKeywordLikePattern")
class SearchKeywordLikePatternTest {

  @Test
  @DisplayName("LIKE wildcard와 escape 문자를 literal contains 패턴으로 변환한다")
  void contains_escapes_like_metacharacters() {
    assertThat(SearchKeywordLikePattern.contains("50%_AB!C"))
        .isEqualTo("%50!%!_AB!!C%");
  }
}
