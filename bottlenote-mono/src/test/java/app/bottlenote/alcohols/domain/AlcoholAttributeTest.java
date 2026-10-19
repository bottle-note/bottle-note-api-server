package app.bottlenote.alcohols.domain;

import static org.assertj.core.api.Assertions.*;

import app.bottlenote.alcohols.constant.AttributeDatatype;
import app.bottlenote.alcohols.constant.AttributeType;
import app.bottlenote.alcohols.exception.AlcoholException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("unit")
@DisplayName("공통 속성 정의와 술별 값")
class AlcoholAttributeTest {
  private AlcoholAttribute price() {
    var attribute =
        AlcoholAttribute.create(
            "bottle_price",
            "병 참고 가격",
            AttributeType.PRICE,
            AttributeDatatype.DOUBLE,
            "원",
            "병 단위 가격");
    ReflectionTestUtils.setField(attribute, "id", 1L);
    return attribute;
  }

  @Test
  void 술별_값을_독립적으로_정규화한다() {
    var attribute = price();
    var first = AlcoholAttributeMapping.create(1L, attribute, " 15000.00 ");
    var second = AlcoholAttributeMapping.create(2L, attribute, "65000");
    assertThat(first.getAttributeValue()).isEqualTo("15000");
    assertThat(second.getAttributeValue()).isEqualTo("65000");
    first.updateValue(attribute.getDatatype(), "16000.10");
    assertThat(first.getAttributeValue()).isEqualTo("16000.1");
    assertThat(second.getAttributeValue()).isEqualTo("65000");
  }

  @Test
  void 키를_보존하고_선택필드를_지운다() {
    var attribute = price();
    attribute.update("새 이름", AttributeType.NOTE, AttributeDatatype.STRING, null, null);
    assertThat(attribute.getAttributeKey()).isEqualTo("bottle_price");
    assertThat(attribute.getUnit()).isNull();
    assertThat(attribute.getDescription()).isNull();
  }

  @Test
  void 검증된_정의로_갱신한다() {
    var attribute = price();
    attribute.updateFrom(
        AlcoholAttribute.create(
            "other", "메모", AttributeType.NOTE, AttributeDatatype.STRING, null, null));
    assertThat(attribute.getAttributeKey()).isEqualTo("bottle_price");
    assertThat(attribute.getDisplayName()).isEqualTo("메모");
    assertThat(attribute.getDatatype()).isEqualTo(AttributeDatatype.STRING);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "1e5", "NaN", "15000원", "1.1234567"})
  void 잘못된_숫자는_기존값을_변경하지_않는다(String value) {
    var mapping = AlcoholAttributeMapping.create(1L, price(), "15000");
    assertThatThrownBy(() -> mapping.updateValue(AttributeDatatype.DOUBLE, value))
        .isInstanceOf(AlcoholException.class);
    assertThat(mapping.getAttributeValue()).isEqualTo("15000");
  }

  @ParameterizedTest
  @ValueSource(strings = {"UPPER", "a-b", "1start", ""})
  void 잘못된_키를_거부한다(String key) {
    assertThatThrownBy(
            () ->
                AlcoholAttribute.create(
                    key, "이름", AttributeType.NOTE, AttributeDatatype.STRING, null, null))
        .isInstanceOf(AlcoholException.class);
  }

  @Test
  void 자료형별_값과_길이를_검증한다() {
    assertThat(AlcoholAttributeMapping.normalizeValue(AttributeDatatype.INT, "00015"))
        .isEqualTo("15");
    assertThat(AlcoholAttributeMapping.normalizeValue(AttributeDatatype.BOOLEAN, " false "))
        .isEqualTo("false");
    assertThat(AlcoholAttributeMapping.normalizeValue(AttributeDatatype.STRING, " 한글 "))
        .isEqualTo("한글");
    assertThatThrownBy(() -> AlcoholAttributeMapping.normalizeValue(AttributeDatatype.INT, "1.2"))
        .isInstanceOf(AlcoholException.class);
    assertThatThrownBy(
            () -> AlcoholAttributeMapping.normalizeValue(AttributeDatatype.BOOLEAN, "yes"))
        .isInstanceOf(AlcoholException.class);
    assertThatThrownBy(
            () -> AlcoholAttributeMapping.normalizeValue(AttributeDatatype.STRING, "x".repeat(256)))
        .isInstanceOf(AlcoholException.class);
    assertThatThrownBy(() -> AlcoholAttributeMapping.normalizeValue(AttributeDatatype.STRING, null))
        .isInstanceOf(AlcoholException.class);
  }

  @Test
  void 정의의_길이를_검증한다() {
    var attribute = price();
    assertThatThrownBy(
            () ->
                attribute.update(
                    "x".repeat(101), AttributeType.PRICE, AttributeDatatype.INT, null, null))
        .isInstanceOf(AlcoholException.class);
    assertThatThrownBy(
            () ->
                attribute.update(
                    "이름", AttributeType.PRICE, AttributeDatatype.INT, "x".repeat(21), null))
        .isInstanceOf(AlcoholException.class);
    assertThat(attribute.getDisplayName()).isEqualTo("병 참고 가격");
  }
}
