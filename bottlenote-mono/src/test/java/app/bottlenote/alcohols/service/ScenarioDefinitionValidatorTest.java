package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.fixture.AlcoholScenarioTestFactory.*;
import static org.assertj.core.api.Assertions.*;

import app.bottlenote.alcohols.constant.*;
import app.bottlenote.alcohols.domain.*;
import app.bottlenote.alcohols.domain.ScenarioDefinition;
import app.bottlenote.alcohols.domain.ScenarioDefinition.*;
import app.bottlenote.alcohols.exception.ScenarioException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("unit")
@DisplayName("시나리오를 검증할 때 초안과 게시 계약을 구분한다")
class ScenarioDefinitionValidatorTest {
  private final ScenarioDefinitionValidator validator = new ScenarioDefinitionValidator();
  private final ObjectMapper mapper =
      new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  private final Map<Long, AlcoholAttribute> attributes = new HashMap<>();
  private final Set<String> codes = Set.of("PEAT", "SWEET");

  @Test
  @DisplayName("서비스에서 슬롯 범위를 벗어난 시나리오를 만들 때 거절한다")
  void 슬롯_범위를_검증할_수_있다() {
    assertThatThrownBy(() -> AlcoholAttributeScenario.create(1L, 0, "시나리오"))
        .isInstanceOf(ScenarioException.class);
    assertThatThrownBy(() -> AlcoholAttributeScenario.create(1L, 4, "시나리오"))
        .isInstanceOf(ScenarioException.class);
    assertThatThrownBy(() -> AlcoholAttributeScenario.create(1L, 1, " "))
        .isInstanceOf(ScenarioException.class);
  }

  @BeforeEach
  void setUp() {
    var experience =
        AlcoholAttribute.create(
            "experience", "경험", AttributeType.KEYWORD, AttributeDatatype.STRING, null, null);
    var price =
        AlcoholAttribute.create(
            "price", "가격", AttributeType.PRICE, AttributeDatatype.INT, "원", null);
    experience.changeGroup(1L);
    price.changeGroup(1L);
    ReflectionTestUtils.setField(experience, "id", 10L);
    ReflectionTestUtils.setField(price, "id", 20L);
    attributes.put(10L, experience);
    attributes.put(20L, price);
  }

  @Test
  @DisplayName("7단계와 동일 속성 질문을 게시할 때 모든 명시적 분기를 허용한다")
  void 독립_질문과_전방_분기를_게시할_수_있다() {
    assertThatCode(() -> validator.validate(sevenStages(10L, 20L), 1L, attributes, codes, true))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("빈 초안을 저장할 때 허용하고 게시할 때 시작 질문 오류를 반환한다")
  void 빈_초안을_저장하고_게시를_거절할_수_있다() {
    assertThatCode(
            () -> validator.validate(ScenarioDefinition.empty(), 1L, attributes, codes, false))
        .doesNotThrowAnyException();
    assertThatThrownBy(
            () -> validator.validate(ScenarioDefinition.empty(), 1L, attributes, codes, true))
        .isInstanceOf(ScenarioException.class)
        .hasMessageContaining("시작 질문");
  }

  static Stream<Arguments> invalidGraphs() {
    return Stream.of(
        test("중복 단계", false, n -> ((ObjectNode) n.at("/stages/1")).put("position", 1)),
        test("중복 분기", false, n -> ((ObjectNode) n.at("/questions/2")).put("branchOrder", 1)),
        test("중복 우선순위", false, n -> ((ObjectNode) n.at("/transitions/1")).put("priority", 1)),
        test("다른 단계", false, n -> ((ObjectNode) n.at("/questions/0")).put("stageId", -99)),
        test("다른 속성", false, n -> ((ObjectNode) n.at("/questions/0")).put("attributeId", 999)),
        test(
            "다른 도착 질문",
            false,
            n -> ((ObjectNode) n.at("/transitions/0")).put("targetQuestionId", 999)),
        test(
            "END의 도착 질문",
            false,
            n -> ((ObjectNode) n.at("/transitions/0")).put("targetKind", "END")),
        test(
            "문구 EQ",
            false,
            n -> ((ObjectNode) n.at("/transitions/0/predicate")).put("value", "다양하게 마셔봤어요")),
        test(
            "선택지 값 타입",
            false,
            n -> ((ObjectNode) n.at("/questions/0/inputConfig/choices/0")).put("value", 123)),
        test(
            "음수 step", false, n -> ((ObjectNode) n.at("/questions/3/inputConfig")).put("step", -1)),
        test("범위 역전", false, n -> ((ObjectNode) n.at("/questions/3/inputConfig")).put("max", -1)),
        test(
            "자료형에 맞지 않는 범위",
            false,
            n -> ((ObjectNode) n.at("/questions/3/inputConfig")).put("min", 0.5)),
        test(
            "자료형 혼합",
            false,
            n -> ((ObjectNode) n.at("/questions/0")).put("inputKind", "NUMBER_RANGE")),
        test("시작 질문 없음", true, n -> n.putNull("startQuestionId")),
        test(
            "뒤로 가는 이동",
            true,
            n -> ((ObjectNode) n.at("/transitions/0")).put("targetQuestionId", -11)),
        test(
            "같은 단계 이동",
            true,
            n -> ((ObjectNode) n.at("/transitions/4")).put("targetQuestionId", -22)),
        test("끊긴 경로", true, n -> ((ArrayNode) n.get("transitions")).remove(15)),
        test(
            "마지막 ALWAYS 없음",
            true,
            n ->
                ((ObjectNode) n.at("/transitions/3/predicate"))
                    .put("operator", "EQ")
                    .put("value", "newbie")),
        test(
            "앞선 ALWAYS",
            true,
            n -> {
              ((ObjectNode) n.at("/transitions/0/predicate"))
                  .put("operator", "ALWAYS")
                  .putNull("value");
            }),
        test(
            "도달 불가", true, n -> ((ObjectNode) n.at("/transitions/2")).put("targetQuestionId", -21)),
        test(
            "중복 EQ",
            true,
            n -> ((ObjectNode) n.at("/transitions/1/predicate")).put("value", "expert")),
        test(
            "미지원 tasting 코드",
            true,
            n -> ((ObjectNode) n.at("/questions/5/inputConfig/choices/0")).put("value", "ABSENT")));
  }

  static Arguments test(String name, boolean publishing, Consumer<ObjectNode> edit) {
    return Arguments.of(name, publishing, edit);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidGraphs")
  @DisplayName("잘못된 정의를 저장하거나 게시할 때 이해 가능한 오류를 반환한다")
  void 잘못된_정의를_거절할_수_있다(String name, boolean publishing, Consumer<ObjectNode> edit)
      throws Exception {
    ObjectNode json = mapper.valueToTree(sevenStages(10L, 20L));
    edit.accept(json);
    var definition = mapper.treeToValue(json, ScenarioDefinition.class);
    assertThatThrownBy(() -> validator.validate(definition, 1L, attributes, codes, publishing))
        .isInstanceOf(ScenarioException.class)
        .hasMessageContaining("시나리오");
  }

  @Test
  @DisplayName("속성 그룹이나 허용값이 바뀔 때 기존 질문을 게시하지 않는다")
  void 다른_그룹과_허용값을_거절할_수_있다() {
    attributes.get(10L).changeGroup(2L);
    assertThatThrownBy(() -> validator.validate(sevenStages(10L, 20L), 1L, attributes, codes, true))
        .hasMessageContaining("질문 -11")
        .hasMessageContaining("현재 그룹");
    attributes.get(10L).changeGroup(1L);
    attributes.get(10L).changeStringRules(List.of("newbie"), null);
    assertThatThrownBy(() -> validator.validate(sevenStages(10L, 20L), 1L, attributes, codes, true))
        .hasMessageContaining("허용값");
  }

  @Test
  @DisplayName("상위 숫자 조건이 뒤 규칙을 모두 가릴 때 해당 규칙 ID로 거절한다")
  void 숫자_조건의_도달_불가를_거절할_수_있다() {
    var q = number(-11, -1, 1, "범위", ScenarioTypes.Source.ABV, null, 0, 100, 1);
    var d =
        new ScenarioDefinition(
            1,
            -11L,
            List.of(new ScenarioStageItem(-1L, 1)),
            List.of(q),
            List.of(
                new ScenarioTransitionItem(
                    -101L,
                    -11L,
                    1,
                    new ScenarioPredicate(ScenarioTypes.Operator.GTE, IntNode.valueOf(20)),
                    ScenarioTypes.Target.END,
                    null),
                new ScenarioTransitionItem(
                    -102L,
                    -11L,
                    2,
                    new ScenarioPredicate(ScenarioTypes.Operator.GTE, IntNode.valueOf(30)),
                    ScenarioTypes.Target.END,
                    null),
                rule(-103, -11, 3, ScenarioTypes.Operator.ALWAYS, null, null)));
    assertThatThrownBy(() -> validator.validate(d, 1L, attributes, codes, true))
        .hasMessageContaining("규칙 -102")
        .hasMessageContaining("가려지거나");
  }

  @Test
  @DisplayName("미지원 JSON 필드와 숫자 문자열을 읽을 때 역직렬화를 거절한다")
  void JSON_타입과_알_수_없는_필드를_거절할_수_있다() {
    assertThatThrownBy(
            () -> mapper.readValue("{\"allowAny\":true,\"min\":\"0\"}", ScenarioInputConfig.class))
        .isInstanceOf(com.fasterxml.jackson.core.JsonProcessingException.class);
    assertThatThrownBy(() -> mapper.readValue("{\"allowAny\":\"true\"}", ScenarioInputConfig.class))
        .isInstanceOf(com.fasterxml.jackson.core.JsonProcessingException.class);
    assertThatThrownBy(
            () ->
                mapper.readValue(
                    "{\"allowAny\":true,\"unsupported\":1}", ScenarioInputConfig.class))
        .isInstanceOf(com.fasterxml.jackson.core.JsonProcessingException.class);
  }
}
