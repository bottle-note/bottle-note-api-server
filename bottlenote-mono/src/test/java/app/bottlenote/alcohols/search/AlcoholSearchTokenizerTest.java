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
  @DisplayName("검색 유의 기호는 문자를 바꾸지 않고 분리만 한다")
  void tokenize_handles_symbols() {
    assertThat(AlcoholSearchTokenizer.tokenize("maker's mark"))
        .containsExactly("maker", "s", "mark");
    assertThat(AlcoholSearchTokenizer.tokenize("Michter’s US*1"))
        .containsExactly("michter", "s", "us", "1");
    assertThat(AlcoholSearchTokenizer.tokenize("Writers' Tears"))
        .containsExactly("writers", "tears");
    assertThat(AlcoholSearchTokenizer.tokenize("Milk & Honey")).containsExactly("milk", "honey");
    assertThat(AlcoholSearchTokenizer.tokenize("Nikka G&G")).containsExactly("nikka", "g&g");
    assertThat(AlcoholSearchTokenizer.tokenize("ex-Bourbon")).containsExactly("ex", "bourbon");
    assertThat(AlcoholSearchTokenizer.tokenize("AD/02.22")).containsExactly("ad", "02", "22");
    assertThat(AlcoholSearchTokenizer.tokenize("No. 7")).containsExactly("no", "7");
    assertThat(AlcoholSearchTokenizer.tokenize("45%")).containsExactly("45%");
    assertThat(AlcoholSearchTokenizer.tokenize("%")).containsExactly("%");
    assertThat(AlcoholSearchTokenizer.tokenize("_")).containsExactly("_");
    assertThat(AlcoholSearchTokenizer.tokenize("single_malt")).containsExactly("single_malt");
    assertThat(AlcoholSearchTokenizer.tokenize("St. Kilian")).containsExactly("st", "kilian");
    assertThat(AlcoholSearchTokenizer.tokenize("Glenfiddich 21y - Winter Storm"))
        .containsExactly("glenfiddich", "21", "y", "winter", "storm");
  }

  @Test
  @DisplayName("한 글자씩 기호로 이어진 약어는 쪼개지 않고 원문 토큰으로 둔다")
  void tokenize_keeps_single_letter_abbreviations() {
    assertThat(AlcoholSearchTokenizer.tokenize("Tullamore D.E.W. XO"))
        .containsExactly("tullamore", "d.e.w", "xo");
    assertThat(AlcoholSearchTokenizer.tokenize("Weller C.Y.P.B."))
        .containsExactly("weller", "c.y.p.b");
    assertThat(AlcoholSearchTokenizer.tokenize("Ben Bracken 10y W&Y"))
        .containsExactly("ben", "bracken", "10", "y", "w&y");
    assertThat(AlcoholSearchTokenizer.tokenize("P.X. Sherry")).containsExactly("p.x", "sherry");
    assertThat(AlcoholSearchTokenizer.tokenize("Mr.Shand’s")).containsExactly("mr", "shand", "s");
    assertThat(AlcoholSearchTokenizer.tokenize("James E.Pepper"))
        .containsExactly("james", "e", "pepper");
    assertThat(AlcoholSearchTokenizer.tokenize("No.C-8106")).containsExactly("no", "c", "8106");
  }

  @Test
  @DisplayName("라틴 확장 문자는 영문과 같은 단어로 묶는다")
  void tokenize_keeps_extended_latin_in_word() {
    assertThat(AlcoholSearchTokenizer.tokenize("Smögen 90 Proof"))
        .containsExactly("smögen", "90", "proof");
    assertThat(AlcoholSearchTokenizer.tokenize("The Glenlivet Nàdurra"))
        .containsExactly("the", "glenlivet", "nàdurra");
    assertThat(AlcoholSearchTokenizer.tokenize("Faß Edition")).containsExactly("faß", "edition");
    assertThat(AlcoholSearchTokenizer.tokenize("惟斯吉 Edition")).containsExactly("惟斯吉", "edition");
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
