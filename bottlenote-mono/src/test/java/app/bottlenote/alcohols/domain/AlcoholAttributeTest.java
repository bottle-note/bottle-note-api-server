package app.bottlenote.alcohols.domain;

import static org.assertj.core.api.Assertions.*;

import app.bottlenote.alcohols.constant.AttributeDatatype;
import app.bottlenote.alcohols.constant.AttributeOperator;
import app.bottlenote.alcohols.constant.AttributeType;
import app.bottlenote.alcohols.exception.AlcoholException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Tag("unit")
@DisplayName("공통 속성")
class AlcoholAttributeTest {
  private AlcoholAttribute price(String value) {
    return AlcoholAttribute.create(
        "price_gte",
        "15,000원 이상",
        AttributeType.PRICE,
        AttributeDatatype.DOUBLE,
        AttributeOperator.GTE,
        value,
        "원",
        "기준");
  }

  @Test
  @DisplayName("가격을 생성할 때 숫자를 정규화하고 비교 조건을 저장한다")
  void 가격을_정규화한다() {
    var a = price(" 15000.00 ");
    assertThat(a.getAttributeValue()).isEqualTo("15000");
    assertThat(a.getOperator()).isEqualTo(AttributeOperator.GTE);
    assertThat(a.getDisplayName()).isEqualTo("15,000원 이상");
  }

  @Test
  @DisplayName("속성을 수정할 때 키를 보존하고 선택 필드를 지운다")
  void 키를_보존한다() {
    var a = price("15000");
    a.update(
        "변경", AttributeType.NOTE, AttributeDatatype.STRING, AttributeOperator.EQ, "메모", null, null);
    assertThat(a.getAttributeKey()).isEqualTo("price_gte");
    assertThat(a.getUnit()).isNull();
    assertThat(a.getDescription()).isNull();
  }

  @Test
  @DisplayName("검증된 속성으로 갱신할 때 기존 키를 유지하고 모든 변경 가능 필드를 적용한다")
  void 검증된_값을_적용한다() {
    var target = price("15000");
    var candidate =
        AlcoholAttribute.create(
            "other_key",
            "메모 속성",
            AttributeType.NOTE,
            AttributeDatatype.STRING,
            AttributeOperator.EQ,
            "변경 내용",
            null,
            null);
    target.updateFrom(candidate);
    assertThat(target.getAttributeKey()).isEqualTo("price_gte");
    assertThat(target.getDisplayName()).isEqualTo("메모 속성");
    assertThat(target.getAttributeType()).isEqualTo(AttributeType.NOTE);
    assertThat(target.getDatatype()).isEqualTo(AttributeDatatype.STRING);
    assertThat(target.getOperator()).isEqualTo(AttributeOperator.EQ);
    assertThat(target.getAttributeValue()).isEqualTo("변경 내용");
    assertThat(target.getUnit()).isNull();
    assertThat(target.getDescription()).isNull();
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "1e5", "NaN", "15000원", "1.1234567"})
  @DisplayName("숫자 값이 형식에 맞지 않을 때 거부한다")
  void 잘못된_숫자를_거부한다(String value) {
    assertThatThrownBy(() -> price(value)).isInstanceOf(AlcoholException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"UPPER", "a-b", "1start", ""})
  @DisplayName("키가 규칙에 맞지 않을 때 거부한다")
  void 잘못된_키를_거부한다(String key) {
    assertThatThrownBy(
            () ->
                AlcoholAttribute.create(
                    key,
                    "이름",
                    AttributeType.KEYWORD,
                    AttributeDatatype.STRING,
                    AttributeOperator.EQ,
                    "smoky",
                    null,
                    null))
        .isInstanceOf(AlcoholException.class);
  }

  @Test
  @DisplayName("문자 속성에 이상 조건을 지정할 때 거부한다")
  void 문자_범위_조건을_거부한다() {
    assertThatThrownBy(
            () ->
                AlcoholAttribute.create(
                    "smoky",
                    "스모키",
                    AttributeType.KEYWORD,
                    AttributeDatatype.STRING,
                    AttributeOperator.GTE,
                    "smoky",
                    null,
                    null))
        .isInstanceOf(AlcoholException.class);
  }

  @Test
  @DisplayName("이름과 단위가 최대 길이를 넘을 때 거부한다")
  void 길이를_검증한다() {
    var a = price("15000");
    assertThatThrownBy(
            () ->
                a.update(
                    "x".repeat(101),
                    AttributeType.PRICE,
                    AttributeDatatype.INT,
                    AttributeOperator.EQ,
                    "1",
                    null,
                    null))
        .isInstanceOf(AlcoholException.class);
    assertThatThrownBy(
            () ->
                a.update(
                    "이름",
                    AttributeType.PRICE,
                    AttributeDatatype.INT,
                    AttributeOperator.EQ,
                    "1",
                    "x".repeat(21),
                    null))
        .isInstanceOf(AlcoholException.class);
    assertThat(a.getAttributeValue()).isEqualTo("15000");
  }
}
