package app.bottlenote.global.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
@DisplayName("[integration] OpenAPI 스펙 문서 노출")
class OpenApiDocsIntegrationTest extends OpenApiSpecTestSupport {

  /**
   * 공통 응답 형식을 쓰지 않고 DTO를 그대로 내보내는 엔드포인트.
   *
   * <p>이 목록은 예외를 허용하기 위한 것이 아니라 예외를 고정하기 위한 것이다. 여기 없는 엔드포인트가 공통 형식을 벗어나면 아래 검사가 실패한다.
   */
  private static final Set<String> BARE_RESPONSE_OPERATIONS =
      Set.of(
          "GET /api/v2/auth/apple/nonce",
          "POST /api/v2/auth/apple",
          "POST /api/v2/auth/kakao",
          "POST /api/v2/auth/agent");

  private static final List<String> ENVELOPE_FIELDS =
      List.of("success", "code", "data", "errors", "meta");

  @Test
  @DisplayName("인증 없이 스펙 문서를 조회할 수 있다")
  void 인증_없이_스펙_문서를_조회할_수_있다() {
    assertThat(mockMvcTester.get().uri(specPath))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.info.title")
        .isEqualTo("보틀노트 Product API");
  }

  @Test
  @DisplayName("스펙 문서는 OpenAPI 3.1 형식이다")
  void 스펙_문서는_OpenAPI_3_1_형식이다() {
    assertThat(mockMvcTester.get().uri(specPath))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.openapi")
        .asString()
        .startsWith("3.1");
  }

  @Test
  @DisplayName("swagger-ui는 비활성화되어 접근할 수 없다")
  void swagger_ui는_비활성화되어_접근할_수_없다() {
    var result = mockMvcTester.get().uri("/swagger-ui/index.html").exchange();

    assertThat(result.getResponse().getStatus()).isNotEqualTo(200);
  }

  @Test
  @DisplayName("공통 형식을 쓰는 엔드포인트의 성공 응답은 공통 필드를 갖는다")
  void 공통_형식을_쓰는_응답은_공통_필드를_갖는다() {
    var envelopeOperations =
        operationsOf(fetchSpec()).stream().filter(operation -> !isBare(operation)).toList();

    assertThat(envelopeOperations).isNotEmpty();
    assertThat(envelopeOperations)
        .allSatisfy(
            operation ->
                assertThat(propertyNamesOf(operation.successSchema()))
                    .withFailMessage("%s 의 성공 응답이 공통 형식이 아닙니다", operation)
                    .containsExactlyInAnyOrderElementsOf(ENVELOPE_FIELDS));
  }

  @Test
  @DisplayName("공통 형식을 쓰지 않는 엔드포인트는 응답 본문이 곧 DTO다")
  void 공통_형식을_쓰지_않는_응답은_DTO를_직접_가리킨다() {
    var bareOperations = operationsOf(fetchSpec()).stream().filter(this::isBare).toList();

    assertThat(bareOperations).hasSameSizeAs(BARE_RESPONSE_OPERATIONS);
    assertThat(bareOperations)
        .allSatisfy(
            operation -> {
              var schema = operation.successSchema();
              assertThat(schema.has("$ref"))
                  .withFailMessage("%s 의 응답이 DTO를 가리키지 않습니다", operation)
                  .isTrue();
              assertThat(propertyNamesOf(schema)).doesNotContainAnyElementsOf(ENVELOPE_FIELDS);
            });
  }

  @Test
  @DisplayName("소셜 로그인은 동의 필요 힌트를 포함한 exact field 응답을 문서화한다")
  void 소셜_로그인은_동의_필요_힌트를_포함한_exact_field_응답을_문서화한다() {
    JsonNode spec = fetchSpec();
    var loginOperations =
        operationsOf(spec).stream()
            .filter(
                operation ->
                    Set.of("POST /api/v2/auth/apple", "POST /api/v2/auth/kakao")
                        .contains(operation.endpoint()))
            .toList();

    assertThat(loginOperations).hasSize(2);
    assertThat(loginOperations)
        .allSatisfy(
            operation -> {
              JsonNode responseSchema = resolveSchema(spec, operation.successSchema());
              assertThat(propertyNamesOf(responseSchema))
                  .containsExactlyInAnyOrder(
                      "accessToken", "isFirstLogin", "nickname", "agreementRequired");
              assertThat(
                      responseSchema
                          .path("properties")
                          .path("agreementRequired")
                          .path("type")
                          .asText())
                  .isEqualTo("boolean");
            });
  }

  @Test
  @DisplayName("동의 조회와 제출은 같은 exact field 응답을 문서화한다")
  void 동의_API는_같은_exact_field_응답을_문서화한다() {
    JsonNode spec = fetchSpec();
    var agreementOperations =
        operationsOf(spec).stream()
            .filter(
                operation ->
                    Set.of("GET /api/v2/agreements/status", "POST /api/v2/agreements")
                        .contains(operation.endpoint()))
            .toList();

    assertThat(agreementOperations).hasSize(2);
    assertThat(agreementOperations)
        .allSatisfy(
            operation -> {
              JsonNode statusSchema =
                  resolveSchema(spec, operation.successSchema().path("properties").path("data"));
              assertThat(propertyNamesOf(statusSchema))
                  .containsExactlyInAnyOrder("eligible", "items");

              JsonNode itemSchema =
                  resolveSchema(spec, statusSchema.path("properties").path("items").path("items"));
              assertThat(propertyNamesOf(itemSchema))
                  .containsExactlyInAnyOrder("type", "required", "agreed", "recordedAt");
            });

    var submitOperation =
        agreementOperations.stream()
            .filter(operation -> operation.endpoint().startsWith("POST "))
            .findFirst()
            .orElseThrow();
    JsonNode requestSchema =
        resolveSchema(
            spec, submitOperation.definition().at("/requestBody/content/application~1json/schema"));
    assertThat(propertyNamesOf(requestSchema)).containsExactly("agreements");
    assertThat(textValuesOf(requestSchema.path("required"))).containsExactly("agreements");
    JsonNode requestItemSchema =
        resolveSchema(spec, requestSchema.path("properties").path("agreements").path("items"));
    assertThat(propertyNamesOf(requestItemSchema))
        .containsExactlyInAnyOrder("type", "action", "content", "inputContext");
    assertThat(textValuesOf(requestItemSchema.path("required")))
        .containsExactlyInAnyOrder("type", "action", "content", "inputContext");
    assertThat(textValuesOf(requestItemSchema.path("properties").path("type").path("enum")))
        .containsExactly("TERMS_OF_SERVICE", "PRIVACY_COLLECTION_USE", "MARKETING");
    assertThat(textValuesOf(requestItemSchema.path("properties").path("action").path("enum")))
        .containsExactly("AGREE", "REVOKE");
  }

  private JsonNode resolveSchema(JsonNode spec, JsonNode schema) {
    String ref = schema.path("$ref").asText();
    return ref.startsWith("#/") ? spec.at(ref.substring(1)) : schema;
  }

  private List<String> textValuesOf(JsonNode array) {
    return StreamSupport.stream(array.spliterator(), false).map(JsonNode::asText).toList();
  }

  private boolean isBare(SpecOperation operation) {
    return BARE_RESPONSE_OPERATIONS.contains(operation.endpoint());
  }
}
