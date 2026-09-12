package app.bottlenote.global.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
@DisplayName("[integration] Product 알코올 상세는 식약처 수입 신고를 문서화하지 않는다")
class AlcoholDetailMfdsOpenApiContractIntegrationTest extends OpenApiSpecTestSupport {

  @Test
  @DisplayName("위스키 상세 스키마와 설명에 mfdsDeclarations를 두지 않는다")
  void 위스키_상세는_수입_신고_필드를_문서화하지_않는다() {
    JsonNode spec = fetchSpec();
    SpecOperation operation =
        operationsOf(spec).stream()
            .filter(candidate -> candidate.endpoint().equals("GET /api/v1/alcohols/{alcoholId}"))
            .findFirst()
            .orElseThrow();

    assertThat(operation.definition().path("description").asText())
        .doesNotContain("mfdsDeclarations", "processedDate", "MFDS");

    JsonNode successSchema = resolve(spec, operation.successSchema());
    JsonNode detail =
        successSchema.path("properties").has("data")
            ? resolve(spec, successSchema.path("properties").path("data"))
            : successSchema;
    assertThat(propertyNamesOf(detail))
        .contains("alcohols", "friendsInfo", "reviewInfo")
        .doesNotContain("mfdsDeclarations");
    assertThat(childNamesOf(spec.at("/components/schemas")).toList())
        .doesNotContain("MfdsPublicDeclarationItem");
  }

  private JsonNode resolve(JsonNode spec, JsonNode schema) {
    String ref = schema.path("$ref").asText();
    return ref.startsWith("#/") ? spec.at(ref.substring(1)) : schema;
  }
}
