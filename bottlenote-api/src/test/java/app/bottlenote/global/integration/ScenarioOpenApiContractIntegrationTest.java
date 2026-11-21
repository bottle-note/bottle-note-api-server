package app.bottlenote.global.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.*;

@Tag("integration")
@DisplayName("사용자 시나리오의 실행 중 OpenAPI 계약을 확인한다")
class ScenarioOpenApiContractIntegrationTest extends OpenApiSpecTestSupport {
  @Test
  @DisplayName("조회와 평가 API는 사용자 질문과 서로 다른 요청·응답 답변 스키마를 문서화한다")
  void publishesUserFacingContract() {
    var spec = fetchSpec();
    for (String endpoint :
        java.util.List.of(
            "GET /api/v1/scenarios/{scenarioId}", "POST /api/v1/scenarios/{scenarioId}/evaluate")) {
      var operation =
          operationsOf(spec).stream()
              .filter(o -> o.endpoint().equals(endpoint))
              .findFirst()
              .orElseThrow();
      var data = resolve(spec, operation.successSchema().at("/properties/data"));
      assertThat(propertyNamesOf(data))
          .contains(
              "scenarioId",
              "versionId",
              "question",
              "answers",
              "candidateCount",
              "candidates",
              "state");
      var question = resolve(spec, data.at("/properties/question"));
      var option = resolve(spec, question.at("/properties/options/items"));
      assertThat(propertyNamesOf(option))
          .contains("optionId", "label", "candidateCount", "selectable", "minExclusive");
      var answer = resolve(spec, data.at("/properties/answers/items"));
      assertThat(propertyNamesOf(answer)).contains("questionId", "optionId", "label", "applied");
      var candidates = resolve(spec, data.at("/properties/candidates"));
      var item = resolve(spec, candidates.at("/properties/content/items"));
      var tags = resolve(spec, item.at("/properties/alcoholsTastingTags"));
      assertThat(propertyNamesOf(item)).contains("alcoholId", "alcoholsTastingTags");
      assertThat(tags.path("type").asText()).isEqualTo("array");
      assertThat(resolve(spec, tags.at("/items")).path("type").asText()).isEqualTo("string");
    }
    var request =
        resolve(
            spec,
            spec.at(
                "/paths/~1api~1v1~1scenarios~1{scenarioId}~1evaluate/post/requestBody/content/application~1json/schema"));
    var answer = resolve(spec, request.at("/properties/answers/items"));
    assertThat(propertyNamesOf(answer)).containsExactlyInAnyOrder("questionId", "optionId");
    assertThat(propertyNamesOf(request))
        .contains("versionId", "answers", "mode", "viewQuestionId", "cursor", "size");
  }

  @Test
  @DisplayName("공개 시나리오 목록은 식별자와 제목만 문서화한다")
  void documentsPublicScenarioList() {
    var spec = fetchSpec();
    var operation =
        operationsOf(spec).stream()
            .filter(o -> o.endpoint().equals("GET /api/v1/scenarios"))
            .findFirst()
            .orElseThrow();
    var data = resolve(spec, operation.successSchema().at("/properties/data"));
    var item = resolve(spec, data.at("/items"));
    assertThat(propertyNamesOf(item)).containsExactlyInAnyOrder("scenarioId", "code", "title");
  }
}
