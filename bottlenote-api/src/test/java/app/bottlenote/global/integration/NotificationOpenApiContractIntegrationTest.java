package app.bottlenote.global.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
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
            "cursor", "size", "eventActions", "groups", "readStatus", "createdFrom", "createdTo");
    assertParameterSchema(operation, "cursor", "string", null);
    assertParameterSchema(operation, "size", "integer", "int32");
    assertThat(parameter(operation, "size").at("/schema").has("minimum")).isTrue();
    assertThat(parameter(operation, "size").at("/schema").has("maximum")).isTrue();
    assertThat(parameter(operation, "size").at("/schema/minimum").asLong()).isEqualTo(1L);
    assertThat(parameter(operation, "size").at("/schema/maximum").asLong()).isEqualTo(100L);
    assertThat(parameter(operation, "eventActions").at("/schema/type").asText()).isEqualTo("array");
    assertThat(parameter(operation, "groups").at("/schema/type").asText()).isEqualTo("array");
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
        .contains("eventAction", "group", "status", "isRead", "createAt", "readAt", "action")
        .doesNotContain("type", "category");
    assertThat(
            StreamSupport.stream(
                    parameter(operation, "eventActions").at("/schema/items/enum").spliterator(),
                    false)
                .map(JsonNode::asText)
                .toList())
        .containsExactly(
            "REVIEW_COMMENT_CREATE",
            "REVIEW_REPLY_CREATE",
            "REVIEW_LIKE_ADD",
            "FOLLOW_CREATE",
            "FOLLOWING_REVIEW_CREATE",
            "REVIEW_FEATURE_SELECT",
            "TASTING_OPEN",
            "TASTING_UPDATE",
            "PROGRAM_OPEN",
            "PROGRAM_UPDATE",
            "PROGRAM_RESULT_ANNOUNCE",
            "NOTICE_PUBLISH",
            "CAMPAIGN_OPEN",
            "HELP_ANSWER_CREATE",
            "REPORT_RESULT_ANNOUNCE",
            "CONTENT_MODERATE",
            "ACCOUNT_STATUS_UPDATE",
            "ALCOHOL_SUBMISSION_REVIEW");
    assertThat(
            StreamSupport.stream(
                    parameter(operation, "groups").at("/schema/items/enum").spliterator(), false)
                .map(JsonNode::asText)
                .toList())
        .contains("REVIEW_AND_FOLLOW", "PROGRAM")
        .hasSize(5);
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

  @Test
  @DisplayName("수신 설정 조회·변경 요청과 응답 schema를 노출한다")
  void 수신_설정_schema를_노출한다() {
    JsonNode spec = fetchSpec();
    SpecOperation get = operation(spec, "GET /api/v1/notifications/settings");
    SpecOperation patch = operation(spec, "PATCH /api/v1/notifications/settings");

    for (SpecOperation operation : java.util.List.of(get, patch)) {
      JsonNode responseSchema =
          resolve(
              spec, operation.definition().at("/responses/200/content/application~1json/schema"));
      JsonNode settingsSchema = resolve(spec, responseSchema.path("properties").path("data"));
      JsonNode groupSchema =
          resolve(spec, settingsSchema.path("properties").path("groups").path("items"));
      assertThat(propertyNamesOf(groupSchema))
          .containsExactlyInAnyOrder("group", "displayName", "settings");
      JsonNode itemSchema =
          resolve(spec, groupSchema.path("properties").path("settings").path("items"));
      assertThat(propertyNamesOf(itemSchema))
          .containsExactlyInAnyOrder(
              "eventAction", "displayName", "description", "defaultEnabled", "enabled");
    }

    JsonNode requestSchema =
        resolve(spec, patch.definition().at("/requestBody/content/application~1json/schema"));
    JsonNode requestItemSchema =
        resolve(spec, requestSchema.path("properties").path("settings").path("items"));
    assertThat(propertyNamesOf(requestItemSchema))
        .containsExactlyInAnyOrder("eventAction", "enabled");
    assertThat(patch.definition().path("description").asText())
        .contains("DUPLICATE_NOTIFICATION_SETTING", "NOTIFICATION_SETTINGS_REQUIRED");
  }

  private SpecOperation operation(JsonNode spec, String endpoint) {
    return operationsOf(spec).stream()
        .filter(candidate -> candidate.endpoint().equals(endpoint))
        .findFirst()
        .orElseThrow();
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
}
