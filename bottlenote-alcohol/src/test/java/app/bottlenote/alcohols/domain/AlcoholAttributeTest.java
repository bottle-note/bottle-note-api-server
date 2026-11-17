package app.bottlenote.alcohols.domain;

import static org.assertj.core.api.Assertions.*;

import app.bottlenote.alcohols.constant.AttributeDatatype;
import app.bottlenote.alcohols.constant.AttributeType;
import app.bottlenote.alcohols.exception.AlcoholException;
import java.util.Arrays;
import java.util.List;
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
    first.updateValue(attribute, "16000.10");
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
    assertThatThrownBy(() -> mapping.updateValue(price(), value))
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

  private AlcoholAttribute stringAttribute() {
    var attribute =
        AlcoholAttribute.create(
            "taste", "맛 평가", AttributeType.KEYWORD, AttributeDatatype.STRING, null, null);
    ReflectionTestUtils.setField(attribute, "id", 2L);
    return attribute;
  }

  @Test
  @DisplayName("선택값과 정규식을 함께 설정할 때 두 규칙으로 생성과 수정을 검증한다")
  void 문자열_규칙을_매핑에_적용한다() {
    var attribute = stringAttribute();
    attribute.changeStringRules(List.of(" 맛있음 ", "적당함", "맛없음"), ".*(음|함)");
    assertThat(attribute.getAllowedValues()).containsExactly("맛있음", "적당함", "맛없음");
    var mapping = AlcoholAttributeMapping.create(1L, attribute, " 맛있음 ");
    assertThat(mapping.getAttributeValue()).isEqualTo("맛있음");
    mapping.updateValue(attribute, "적당함");
    assertThatThrownBy(() -> mapping.updateValue(attribute, "아주 맛있음"))
        .isInstanceOf(AlcoholException.class);
    assertThat(mapping.getAttributeValue()).isEqualTo("적당함");
    assertThatThrownBy(() -> AlcoholAttributeMapping.create(1L, attribute, "기타"))
        .isInstanceOf(AlcoholException.class);
    attribute.changeStringRules(null, null);
    mapping.updateValue(attribute, "기타");
    assertThat(mapping.getAttributeValue()).isEqualTo("기타");
  }

  @Test
  @DisplayName("정규식만 설정할 때 부분 일치를 거부하고 전체 값을 검사한다")
  void 정규식_전체_일치를_검증한다() {
    var attribute = stringAttribute();
    attribute.changeStringRules(null, "[A-Z]{2}[0-9]{2}");
    assertThat(AlcoholAttributeMapping.create(1L, attribute, "AB12").getAttributeValue())
        .isEqualTo("AB12");
    assertThatThrownBy(() -> AlcoholAttributeMapping.create(1L, attribute, "XAB12X"))
        .isInstanceOf(AlcoholException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"*", "[", "", " "})
  @DisplayName("잘못된 정규식을 설정할 때 기존 규칙을 보존한다")
  void 잘못된_정규식을_거부한다(String pattern) {
    var attribute = stringAttribute();
    attribute.changeStringRules(List.of("맛있음"), null);
    assertThatThrownBy(() -> attribute.changeStringRules(null, pattern))
        .isInstanceOf(AlcoholException.class);
    assertThat(attribute.getAllowedValues()).containsExactly("맛있음");
    assertThat(attribute.getPattern()).isNull();
  }

  @Test
  @DisplayName("빈 목록·중복·공백·null·초과 길이·정규식과 충돌하는 선택값을 거부한다")
  void 잘못된_선택값을_거부한다() {
    var attribute = stringAttribute();
    for (var values :
        List.of(
            List.<String>of(),
            List.of("a", " a "),
            List.of(" "),
            Arrays.asList("a", null),
            List.of("x".repeat(256)),
            java.util.stream.IntStream.range(0, 101).mapToObj(String::valueOf).toList())) {
      assertThatThrownBy(() -> attribute.changeStringRules(values, null))
          .isInstanceOf(AlcoholException.class);
    }
    assertThatThrownBy(() -> attribute.changeStringRules(List.of("맛있음"), "[A-Z]+"))
        .isInstanceOf(AlcoholException.class);
    assertThatThrownBy(() -> attribute.changeStringRules(null, "x".repeat(501)))
        .isInstanceOf(AlcoholException.class);
  }

  @Test
  @DisplayName("문자열 외 자료형에 규칙을 설정할 때 거부한다")
  void 문자열_외_규칙을_거부한다() {
    var attribute = price();
    assertThatThrownBy(() -> attribute.changeStringRules(List.of("1"), null))
        .isInstanceOf(AlcoholException.class);
    assertThatThrownBy(() -> attribute.changeStringRules(null, ".*"))
        .isInstanceOf(AlcoholException.class);
  }

  @Test
  @DisplayName("허용값 100개와 값 255자 및 정규식 500자를 설정할 때 저장한다")
  void 문자열_규칙의_최대_길이를_허용한다() {
    var attribute = stringAttribute();
    var values =
        java.util.stream.IntStream.range(0, 100)
            .mapToObj(i -> String.valueOf(i) + "x".repeat(253))
            .toList();
    attribute.changeStringRules(values, "(?x).*" + " ".repeat(494));
  }
}
