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
            "cursor", "size", "types", "categories", "readStatus", "createdFrom", "createdTo");
    assertParameterSchema(operation, "cursor", "string", null);
    assertParameterSchema(operation, "size", "integer", "int32");
    assertThat(parameter(operation, "size").at("/schema").has("minimum")).isTrue();
    assertThat(parameter(operation, "size").at("/schema").has("maximum")).isTrue();
    assertThat(parameter(operation, "size").at("/schema/minimum").asLong()).isEqualTo(1L);
    assertThat(parameter(operation, "size").at("/schema/maximum").asLong()).isEqualTo(100L);
    assertThat(parameter(operation, "types").at("/schema/type").asText()).isEqualTo("array");
    assertThat(parameter(operation, "categories").at("/schema/type").asText()).isEqualTo("array");
    assertParameterSchema(operation, "readStatus", "string", null);
    assertThat(
            StreamSupport.stream(
                    parameter(operation, "readStatus").at("/schema/enum").spliterator(), false)
                .map(JsonNode::asText)
                .toList())
        .containsExactly("ALL", "UNREAD", "READ");
    assertParameterSchema(operation, "createdFrom", "string", "date-time");
    assertParameterSchema(operation, "createdTo", "string", "date-time");

    JsonNode responseSchema =
        resolve(spec, operation.definition().at("/responses/200/content/application~1json/schema"));
    JsonNode listSchema = resolve(spec, responseSchema.path("properties").path("data"));
    JsonNode itemSchema = resolve(spec, listSchema.path("properties").path("items").path("items"));
    assertThat(propertyNamesOf(itemSchema))
        .contains("status", "isRead", "createAt", "readAt", "action");
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
    assertThat(actionSchema.path("properties").path("version").path("type").asText())
        .isEqualTo("integer");
    JsonNode actionTypeSchema = resolve(spec, actionSchema.path("properties").path("type"));
    assertThat(
            StreamSupport.stream(actionTypeSchema.path("enum").spliterator(), false)
                .map(JsonNode::asText)
                .toList())
        .contains("OPEN_REVIEW", "OPEN_HELP", "OPEN_USER");
    JsonNode payloadSchema = actionSchema.path("properties").path("payload");
    if (payloadSchema.has("$ref")) {
      payloadSchema = spec.at(payloadSchema.path("$ref").asText().substring(1));
    }
    JsonNode payloadVariants = payloadSchema.path("anyOf");
    assertThat(payloadVariants.isArray()).isTrue();
    assertThat(payloadSchema.has("oneOf")).isFalse();
    assertThat(
            StreamSupport.stream(payloadVariants.spliterator(), false)
                .map(variant -> variant.path("$ref").asText())
                .toList())
        .contains(
            "#/components/schemas/OpenReviewActionPayload",
            "#/components/schemas/OpenReviewDetailActionPayload",
            "#/components/schemas/OpenHelpActionPayload",
            "#/components/schemas/OpenUserActionPayload");
    assertThat(spec.at("/components/schemas/OpenReviewActionPayload").has("allOf")).isFalse();
    assertThat(
            spec.at("/components/schemas/OpenReviewActionPayload/properties").properties().stream()
                .map(java.util.Map.Entry::getKey)
                .toList())
        .containsExactly("replyId");
    assertThat(spec.at("/components/schemas/OpenReviewDetailActionPayload/properties").isEmpty())
        .isTrue();
    assertThat(spec.at("/components/schemas/OpenHelpActionPayload/properties").isEmpty()).isTrue();
    assertThat(spec.at("/components/schemas/OpenUserActionPayload/properties").isEmpty()).isTrue();
    assertThat(operation.definition().path("description").asText())
        .contains("OPEN_REVIEW` v1", "OPEN_REVIEW` v2", "OPEN_USER` v1");
  }

  private void assertParameterSchema(
      SpecOperation operation, String name, String type, String format) {
    JsonNode schema = parameter(operation, name).path("schema");
    assertThat(schema.path("type").asText()).isEqualTo(type);
    if (format != null) {
      assertThat(schema.path("format").asText()).isEqualTo(format);
    }
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
