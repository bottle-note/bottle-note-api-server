package app.bottlenote.global.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
@DisplayName("[integration] Product 알코올 상세 description OpenAPI 계약")
class AlcoholDetailDescriptionOpenApiContractIntegrationTest extends OpenApiSpecTestSupport {

  @Test
  @DisplayName("위스키 상세 alcohols 스키마에 description을 문서화한다")
  void 상세_스키마에_description을_문서화한다() {
    JsonNode spec = fetchSpec();
    SpecOperation operation =
        operationsOf(spec).stream()
            .filter(candidate -> candidate.endpoint().equals("GET /api/v1/alcohols/{alcoholId}"))
            .findFirst()
            .orElseThrow();

    JsonNode envelope = resolve(spec, operation.successSchema());
    JsonNode detail = resolve(spec, envelope.path("properties").path("data"));
    JsonNode alcohols = resolve(spec, detail.path("properties").path("alcohols"));
    JsonNode description = alcohols.path("properties").path("description");

    assertThat(description.isMissingNode()).isFalse();
    assertThat(description.path("type").asText()).isEqualTo("string");
    assertThat(propertyNamesOf(alcohols)).contains("description");
  }
}
