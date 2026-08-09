package app.bottlenote.global.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
@DisplayName("[integration] Notification OpenAPI 계약")
class NotificationOpenApiContractIntegrationTest extends OpenApiSpecTestSupport {

  @Test
  @DisplayName("목록 query와 Action 읽음 시각 schema를 노출한다")
  void 알림_목록_query와_Action_읽음_시각_schema를_노출한다() {
    JsonNode spec = fetchSpec();
    SpecOperation operation =
        operationsOf(spec).stream()
            .filter(candidate -> candidate.endpoint().equals("GET /api/v1/notifications"))
            .findFirst()
            .orElseThrow();

    assertThat(
            StreamSupport.stream(operation.definition().path("parameters").spliterator(), false)
                .map(parameter -> parameter.path("name").asText())
                .toList())
        .containsExactlyInAnyOrder(
            "cursor", "pageSize", "types", "categories", "readStatus", "createdFrom", "createdTo");
    assertThat(parameter(operation, "types").at("/schema/type").asText()).isEqualTo("array");
    assertThat(parameter(operation, "categories").at("/schema/type").asText()).isEqualTo("array");

    JsonNode responseSchema =
        resolve(
            spec,
            operation
                .definition()
                .at("/responses/200/content/application~1json/schema"));
    JsonNode listSchema = resolve(spec, responseSchema.path("properties").path("data"));
    JsonNode itemSchema =
        resolve(spec, listSchema.path("properties").path("items").path("items"));
    assertThat(propertyNamesOf(itemSchema)).contains("status", "isRead", "createAt", "readAt", "action");
    assertThat(itemSchema.path("properties").path("status").path("description").asText())
        .contains("전달 상태", "읽음 여부와 무관");
    assertThat(itemSchema.path("properties").path("createAt").path("description").asText())
        .contains("+09:00");
    assertThat(itemSchema.path("properties").path("readAt").path("description").asText())
        .contains("null", "+09:00");

    JsonNode actionSchema = resolve(spec, itemSchema.path("properties").path("action"));
    assertThat(propertyNamesOf(actionSchema))
        .containsExactlyInAnyOrder("type", "targetId", "payload", "version", "fallbackType");
    assertThat(actionSchema.path("properties").path("fallbackType").path("description").asText())
        .contains("fallback");
    JsonNode actionTypeSchema =
        resolve(spec, actionSchema.path("properties").path("type"));
    assertThat(
            StreamSupport.stream(actionTypeSchema.path("enum").spliterator(), false)
                .map(JsonNode::asText)
                .toList())
        .contains("OPEN_REVIEW", "OPEN_HELP");
    JsonNode payloadSchema =
        resolve(spec, actionSchema.path("properties").path("payload"));
    assertThat(propertyNamesOf(payloadSchema)).containsExactly("replyId");
    assertThat(spec.at("/components/schemas/OpenHelpActionPayload/properties").isEmpty()).isTrue();
  }

  private JsonNode parameter(SpecOperation operation, String name) {
    return StreamSupport.stream(operation.definition().path("parameters").spliterator(), false)
        .filter(parameter -> parameter.path("name").asText().equals(name))
        .findFirst()
        .orElseThrow();
  }

  private JsonNode resolve(JsonNode spec, JsonNode schema) {
    JsonNode candidate =
        List.of("anyOf", "oneOf").stream()
            .map(schema::path)
            .filter(JsonNode::isArray)
            .flatMap(composition -> StreamSupport.stream(composition.spliterator(), false))
            .filter(node -> node.has("$ref"))
            .findFirst()
            .orElse(schema);
    String ref = candidate.path("$ref").asText();
    return ref.startsWith("#/") ? spec.at(ref.substring(1)) : candidate;
  }
}
