package app.bottlenote.alcohols.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("AlcoholSearchTokenizer")
class AlcoholSearchTokenizerTest {

  @Test
  @DisplayName("null 또는 공백 입력은 빈 토큰 목록을 반환한다")
  void tokenize_blank_returns_empty() {
    assertThat(AlcoholSearchTokenizer.tokenize(null)).isEmpty();
    assertThat(AlcoholSearchTokenizer.tokenize("   ")).isEmpty();
  }

  @Test
  @DisplayName("공백 분리와 소문자화, 입력 순서 중복 제거를 수행한다")
  void tokenize_splits_whitespace_lowercases_and_deduplicates() {
    assertThat(AlcoholSearchTokenizer.tokenize("  PEAT   맥캘란 peat  "))
        .containsExactly("peat", "맥캘란");
  }

  @Test
  @DisplayName("한글·숫자 경계와 영문·숫자 경계를 분리한다")
  void tokenize_splits_script_and_digit_boundaries() {
    assertThat(AlcoholSearchTokenizer.tokenize("발베니12")).containsExactly("발베니", "12");
    assertThat(AlcoholSearchTokenizer.tokenize("발베니 12")).containsExactly("발베니", "12");
    assertThat(AlcoholSearchTokenizer.tokenize("macallan12")).containsExactly("macallan", "12");
    assertThat(AlcoholSearchTokenizer.tokenize("12년")).containsExactly("12", "년");
    assertThat(AlcoholSearchTokenizer.tokenize("14yo")).containsExactly("14", "yo");
    assertThat(AlcoholSearchTokenizer.tokenize("14y")).containsExactly("14", "y");
  }

  @Test
  @DisplayName("검색 유의 기호는 분리하고 아포스트로피는 제거 후 접합한다")
  void tokenize_handles_symbols() {
    assertThat(AlcoholSearchTokenizer.tokenize("maker's mark")).containsExactly("makers", "mark");
    assertThat(AlcoholSearchTokenizer.tokenize("Milk & Honey"))
        .containsExactly("milk", "and", "honey");
    assertThat(AlcoholSearchTokenizer.tokenize("ex-Bourbon")).containsExactly("ex", "bourbon");
    assertThat(AlcoholSearchTokenizer.tokenize("AD/02.22")).containsExactly("ad", "02", "22");
    assertThat(AlcoholSearchTokenizer.tokenize("No. 7")).containsExactly("no", "7");
    assertThat(AlcoholSearchTokenizer.tokenize("45%")).containsExactly("45");
    assertThat(AlcoholSearchTokenizer.tokenize("St. Kilian")).containsExactly("st", "kilian");
    assertThat(AlcoholSearchTokenizer.tokenize("Glenfiddich 21y - Winter Storm"))
        .containsExactly("glenfiddich", "21", "y", "winter", "storm");
  }

  @Test
  @DisplayName("keywords 리스트 입력은 각 원소를 재토큰화한 뒤 순서 보존으로 합친다")
  void tokenizeAll_flattens_each_part() {
    assertThat(AlcoholSearchTokenizer.tokenizeAll(List.of("발베니12", "Double-Wood", "peat peat")))
        .containsExactly("발베니", "12", "double", "wood", "peat");
  }

  @Test
  @DisplayName("문서 필드는 동일 토큰 규칙으로 공백 결합 검색 텍스트를 만든다")
  void normalizeDocument_uses_same_token_rules() {
    assertThat(
            AlcoholSearchTokenizer.normalizeDocument(
                "발베니 더블우드 12년", "Balvenie 12y DoubleWood", "싱글 몰트"))
        .isEqualTo("발베니 더블우드 12 년 balvenie y doublewood 싱글 몰트");
  }
}
