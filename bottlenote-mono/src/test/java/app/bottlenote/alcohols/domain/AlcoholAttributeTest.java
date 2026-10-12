package app.bottlenote.alcohols.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.alcohols.constant.AttributeDatatype;
import app.bottlenote.alcohols.constant.AttributeType;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.alcohols.exception.AlcoholExceptionCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Tag("unit")
@DisplayName("위스키 속성 도메인 단위 테스트")
@SuppressWarnings("NonAsciiCharacters")
class AlcoholAttributeTest {

  @Test
  @DisplayName("항목과 값을 다듬어 속성을 만든다")
  void 속성을_만들_수_있다() {
    AlcoholAttribute attribute =
        AlcoholAttribute.create(
            247L,
            " keyword ",
            AttributeType.KEYWORD,
            AttributeDatatype.STRING,
            " 입문 ",
            "  추천 필터 ",
            null);

    assertThat(attribute.getAlcoholId()).isEqualTo(247L);
    assertThat(attribute.getAttributeKey()).isEqualTo("keyword");
    assertThat(attribute.getAttributeValue()).isEqualTo("입문");
    assertThat(attribute.getDescription()).isEqualTo("추천 필터");
    assertThat(attribute.hasSameKeyAndValue("keyword", "입문")).isTrue();
  }

  @ParameterizedTest(name = "key=\"{0}\"")
  @CsvSource({"Keyword", "1abc", "price-bottle", "''", "' '"})
  @DisplayName("항목이 영문 소문자·숫자·밑줄 형식이 아니면 거부한다")
  void 잘못된_항목은_거부한다(String key) {
    assertThatThrownBy(
            () ->
                AlcoholAttribute.create(
                    1L, key, AttributeType.KEYWORD, AttributeDatatype.STRING, "값", null, null))
        .isInstanceOf(AlcoholException.class)
        .hasMessage(AlcoholExceptionCode.ALCOHOL_ATTRIBUTE_INVALID_KEY.getMessage());
  }

  @ParameterizedTest(name = "{0} <- \"{1}\"")
  @CsvSource({
    "INT, 58000, true",
    "INT, 58000.5, false",
    "DOUBLE, 86.50, true",
    "DOUBLE, abc, false",
    "BOOLEAN, true, true",
    "BOOLEAN, yes, false",
    "STRING, 60도 이상, true"
  })
  @DisplayName("값은 입력 형식에 맞을 때만 받는다")
  void 값은_형식에_맞아야_한다(AttributeDatatype datatype, String value, boolean accepted) {
    if (accepted) {
      AlcoholAttribute attribute =
          AlcoholAttribute.create(
              1L, "score", AttributeType.POPULARITY, datatype, value, null, null);
      String expected =
          datatype.isNumeric() ? new BigDecimal(value).stripTrailingZeros().toPlainString() : value;
      assertThat(attribute.getAttributeValue()).isEqualTo(expected);
      return;
    }
    assertThatThrownBy(
            () ->
                AlcoholAttribute.create(
                    1L, "score", AttributeType.POPULARITY, datatype, value, null, null))
        .isInstanceOf(AlcoholException.class)
        .hasMessage(AlcoholExceptionCode.ALCOHOL_ATTRIBUTE_INVALID_VALUE.getMessage());
  }

  @Test
  @DisplayName("빈 값은 형식과 무관하게 거부한다")
  void 빈_값은_거부한다() {
    assertThatThrownBy(
            () ->
                AlcoholAttribute.create(
                    1L, "note", AttributeType.NOTE, AttributeDatatype.STRING, "   ", null, null))
        .isInstanceOf(AlcoholException.class)
        .hasMessage(AlcoholExceptionCode.ALCOHOL_ATTRIBUTE_INVALID_VALUE.getMessage());
  }

  @Test
  @DisplayName("소수 끝의 0을 떼어 40.0을 40으로 저장한다")
  void 숫자_끝의_0을_떼어_저장한다() {
    AlcoholAttribute attribute =
        AlcoholAttribute.create(
            1L, "abv", AttributeType.POPULARITY, AttributeDatatype.DOUBLE, "40.0", null, null);

    assertThat(attribute.getAttributeValue()).isEqualTo("40");
  }

  @Test
  @DisplayName("수정은 항목을 두고 분류·형식·값·근거·기준 시점만 바꾼다")
  void 수정하면_항목은_유지된다() {
    AlcoholAttribute attribute =
        AlcoholAttribute.create(
            1L, "score", AttributeType.POPULARITY, AttributeDatatype.DOUBLE, "80", "초기", null);
    LocalDateTime basedAt = LocalDateTime.of(2026, 9, 28, 0, 0);

    attribute.update(AttributeType.POPULARITY, AttributeDatatype.DOUBLE, "86.5", "데일리샷", basedAt);

    assertThat(attribute.getAttributeKey()).isEqualTo("score");
    assertThat(attribute.getAttributeValue()).isEqualTo("86.5");
    assertThat(attribute.getDescription()).isEqualTo("데일리샷");
    assertThat(attribute.getBasedAt()).isEqualTo(basedAt);
  }

  @Test
  @DisplayName("수정 값이 형식에 맞지 않으면 기존 값을 유지한다")
  void 수정_실패_시_기존_값을_유지한다() {
    AlcoholAttribute attribute =
        AlcoholAttribute.create(
            1L, "score", AttributeType.POPULARITY, AttributeDatatype.DOUBLE, "80", null, null);

    assertThatThrownBy(
            () ->
                attribute.update(
                    AttributeType.POPULARITY, AttributeDatatype.INT, "8.5", null, null))
        .isInstanceOf(AlcoholException.class);
    assertThat(attribute.getAttributeValue()).isEqualTo("80");
    assertThat(attribute.getDatatype()).isEqualTo(AttributeDatatype.DOUBLE);
  }
}
