package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.fixture.AlcoholScenarioTestFactory.*;
import static org.assertj.core.api.Assertions.*;

import app.bottlenote.alcohols.dto.request.AdminScenarioPreviewRequest;
import app.bottlenote.alcohols.dto.request.ScenarioDefinitionRequest;
import app.bottlenote.alcohols.dto.response.ScenarioAnswerItem;
import app.bottlenote.alcohols.dto.response.ScenarioDefinitionResponse;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Tag("unit")
@DisplayName("시나리오 입출력 모델을 분리할 때 JSON 계약과 도메인 변환을 보존한다")
class ScenarioDtoContractTest {
  private final ObjectMapper mapper =
      new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  @Test
  @DisplayName("분기와 중복 속성이 있는 정의를 변환할 때 모든 필드를 보존한다")
  void 정의를_손실_없이_변환할_수_있다() throws Exception {
    var original = mapper.<JsonNode>valueToTree(sevenStages(10L, 20L));
    var request = mapper.treeToValue(original, ScenarioDefinitionRequest.class);
    var response = ScenarioDefinitionResponse.from(request.toDefinition());
    assertThat(mapper.<JsonNode>valueToTree(response)).isEqualTo(original);
    assertThat(
            mapper.<JsonNode>valueToTree(
                mapper
                    .treeToValue(
                        mapper.<JsonNode>valueToTree(response), ScenarioDefinitionRequest.class)
                    .toDefinition()))
        .isEqualTo(original);
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
    assertThat(
            mapper.<JsonNode>valueToTree(
                ScenarioAnswerItem.from(request.toInput().answers().getFirst())))
        .isEqualTo(mapper.<JsonNode>valueToTree(request.answers().getFirst()));
  }

  @Test
  @DisplayName("빈 초안과 잘못된 배열을 변환할 때 서비스 검증에 필요한 값을 유지한다")
  void 초안_검증_입력을_보존할_수_있다() throws Exception {
    assertThat(ScenarioDefinitionRequest.empty().toDefinition().questions()).isEmpty();
    var request =
        mapper.readValue(
            "{\"definitionSchemaVersion\":1,\"stages\":null,\"questions\":[null],\"transitions\":[]}",
            ScenarioDefinitionRequest.class);
    assertThat(request.toDefinition().stages()).isNull();
    assertThat(request.toDefinition().questions())
        .containsExactly(
            (app.bottlenote.alcohols.domain.ScenarioDefinition.ScenarioQuestionItem) null);
  }
}
