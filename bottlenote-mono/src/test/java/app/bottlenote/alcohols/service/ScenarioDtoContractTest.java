package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.fixture.AlcoholScenarioTestFactory.*;
import static org.assertj.core.api.Assertions.*;

import app.bottlenote.alcohols.domain.ScenarioAnswer;
import app.bottlenote.alcohols.domain.ScenarioDefinition;
import app.bottlenote.alcohols.dto.request.AdminScenarioPreviewRequest;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Tag("unit")
@DisplayName("시나리오 공통 모델을 사용할 때 JSON 계약을 보존한다")
class ScenarioDtoContractTest {
  private final ObjectMapper mapper =
      new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  @Test
  @DisplayName("분기와 중복 속성이 있는 정의를 변환할 때 모든 필드를 보존한다")
  void 정의를_손실_없이_변환할_수_있다() throws Exception {
    var original = mapper.<JsonNode>valueToTree(sevenStages(10L, 20L));
    var request = mapper.treeToValue(original, ScenarioDefinition.class);
    assertThat(mapper.<JsonNode>valueToTree(request)).isEqualTo(original);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{\"questionId\":1,\"kind\":\"CHOICE\",\"choiceId\":\"stable\"}",
        "{\"questionId\":1,\"kind\":\"NUMBER_RANGE\",\"min\":1.5,\"max\":2.5}",
        "{\"questionId\":1,\"kind\":\"ANY\"}"
      })
  @DisplayName("답변 종류별 요청을 변환할 때 응답 필드와 JSON 타입을 보존한다")
  void 답변을_손실_없이_변환할_수_있다(String json) throws Exception {
    var request =
        mapper.readValue(
            "{\"expectedLockVersion\":0,\"answers\":[" + json + "]}",
            AdminScenarioPreviewRequest.class);
    var expected = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(json);
    for (String optional : java.util.List.of("choiceId", "min", "max")) {
      if (!expected.has(optional)) expected.putNull(optional);
    }
    assertThat(mapper.readTree(mapper.writeValueAsString(request.answers().getFirst())))
        .isEqualTo(expected);
    assertThat(request.page()).isZero();
    assertThat(request.size()).isEqualTo(25);
  }

  @Test
  @DisplayName("빈 초안과 잘못된 배열을 변환할 때 서비스 검증에 필요한 값을 유지한다")
  void 초안_검증_입력을_보존할_수_있다() throws Exception {
    assertThat(ScenarioDefinition.empty().questions()).isEmpty();
    var request =
        mapper.readValue(
            "{\"definitionSchemaVersion\":1,\"stages\":null,\"questions\":[null],\"transitions\":[]}",
            ScenarioDefinition.class);
    assertThat(request.stages()).isNull();
    assertThat(request.questions())
        .containsExactly(
            (app.bottlenote.alcohols.domain.ScenarioDefinition.ScenarioQuestionItem) null);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{\"questionId\":1,\"kind\":\"NUMBER_RANGE\",\"min\":\"1\",\"max\":2}",
        "{\"questionId\":1,\"kind\":\"NUMBER_RANGE\",\"min\":1,\"max\":\"2\"}",
        "{\"questionId\":1,\"kind\":\"NUMBER_RANGE\",\"min\":true,\"max\":2}",
        "{\"questionId\":1,\"kind\":0}",
        "{\"questionId\":1,\"kind\":\"ANY\",\"unsupported\":true}"
      })
  @DisplayName("공통 답변에 미지원 필드나 잘못된 scalar 타입을 보내면 거절한다")
  void 잘못된_답변_JSON을_거절한다(String json) {
    assertThatThrownBy(() -> mapper.readValue(json, ScenarioAnswer.class))
        .isInstanceOf(com.fasterxml.jackson.core.JsonProcessingException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "1", "true", "\"0\""})
  @DisplayName("공통 출처에 enum 숫자나 boolean을 보내면 후보와 정의 입력 모두 거절한다")
  void 출처_코드는_문자열_enum만_허용한다(String json) {
    assertThatThrownBy(
            () ->
                mapper.readValue(
                    "{\"source\":" + json + "}",
                    app.bottlenote.alcohols.dto.request.AdminAlcoholCandidateRequest.NextQuestion
                        .class))
        .isInstanceOf(com.fasterxml.jackson.core.JsonProcessingException.class);
    assertThatThrownBy(
            () ->
                mapper.readValue(
                    "{\"sourceKind\":" + json + "}", ScenarioDefinition.ScenarioQuestionItem.class))
        .isInstanceOf(com.fasterxml.jackson.core.JsonProcessingException.class);
  }
}
