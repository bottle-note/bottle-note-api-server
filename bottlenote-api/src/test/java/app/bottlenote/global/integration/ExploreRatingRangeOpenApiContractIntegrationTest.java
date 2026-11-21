package app.bottlenote.global.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
@DisplayName("[integration] 둘러보기 별점 범위 OpenAPI 계약")
class ExploreRatingRangeOpenApiContractIntegrationTest extends OpenApiSpecTestSupport {

  @Test
  @DisplayName("위스키와 리뷰 둘러보기는 ratingFrom과 ratingTo 포함 범위를 문서화한다")
  void 둘러보기는_별점_포함_범위를_문서화한다() {
    JsonNode spec = fetchSpec();

    assertRatingRange(spec, "/api/v1/alcohols/explore/standard");
    assertRatingRange(spec, "/api/v1/reviews/explore/standard");
  }

  private void assertRatingRange(JsonNode spec, String path) {
    JsonNode parameters = spec.path("paths").path(path).path("get").path("parameters");
    JsonNode ratingFrom = parameter(parameters, "ratingFrom");
    JsonNode ratingTo = parameter(parameters, "ratingTo");

    assertThat(parameterNames(parameters))
        .contains("ratingFrom", "ratingTo")
        .doesNotContain("rating");
    assertBoundarySchema(ratingFrom.path("schema"));
    assertBoundarySchema(ratingTo.path("schema"));
    assertThat(ratingFrom.path("description").asText()).contains("포함 하한");
    assertThat(ratingTo.path("description").asText()).contains("포함 상한");
  }

  private JsonNode parameter(JsonNode parameters, String name) {
    return StreamSupport.stream(parameters.spliterator(), false)
        .filter(parameter -> parameter.path("name").asText().equals(name))
        .findFirst()
        .orElseThrow();
  }

  private java.util.List<String> parameterNames(JsonNode parameters) {
    return StreamSupport.stream(parameters.spliterator(), false)
        .map(parameter -> parameter.path("name").asText())
        .toList();
  }

  private void assertBoundarySchema(JsonNode schema) {
    assertThat(schema.path("minimum").decimalValue()).isEqualByComparingTo("0.5");
    assertThat(schema.path("maximum").decimalValue()).isEqualByComparingTo("5.0");
    assertThat(schema.path("multipleOf").decimalValue()).isEqualByComparingTo("0.5");
  }
}
