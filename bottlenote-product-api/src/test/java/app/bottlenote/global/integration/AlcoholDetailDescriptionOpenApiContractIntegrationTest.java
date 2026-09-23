package app.bottlenote.global.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.stream.StreamSupport;
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
    assertThat(hasType(description, "string")).isTrue();
    assertThat(isNullable(description)).isTrue();
    assertThat(propertyNamesOf(alcohols)).contains("description");
  }

  private boolean isNullable(JsonNode schema) {
    return schema.path("nullable").asBoolean(false)
        || hasType(schema, "null")
        || compositionHasType(schema, "null");
  }

  private boolean hasType(JsonNode schema, String expected) {
    JsonNode type = schema.path("type");
    if (type.isTextual()) {
      return type.asText().equals(expected);
    }
    return type.isArray()
        && StreamSupport.stream(type.spliterator(), false)
            .anyMatch(candidate -> candidate.asText().equals(expected));
  }

  private boolean compositionHasType(JsonNode schema, String expected) {
    for (String compositionName : new String[] {"anyOf", "oneOf"}) {
      JsonNode composition = schema.path(compositionName);
      if (composition.isArray()
          && StreamSupport.stream(composition.spliterator(), false)
              .anyMatch(candidate -> hasType(candidate, expected))) {
        return true;
      }
    }
    return false;
  }
}
