package app.bottlenote.global.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
@DisplayName("[integration] ReviewExplore OpenAPI 위치 계약")
class ReviewExploreOpenApiContractIntegrationTest extends OpenApiSpecTestSupport {

  @Test
  @DisplayName("리뷰 둘러보기는 nullable locationInfo와 전체 위치 필드를 문서화한다")
  void 리뷰_둘러보기는_nullable_locationInfo와_전체_위치_필드를_문서화한다() {
    JsonNode spec = fetchSpec();
    SpecOperation operation =
        operationsOf(spec).stream()
            .filter(
                candidate -> candidate.endpoint().equals("GET /api/v1/reviews/explore/standard"))
            .findFirst()
            .orElseThrow();

    assertThat(operation.definition().path("description").asText())
        .contains("locationInfo")
        .contains("null");

    JsonNode responseData =
        resolve(spec, operation.successSchema().path("properties").path("data"));
    JsonNode item = resolve(spec, responseData.path("properties").path("items").path("items"));
    JsonNode locationInfo = resolve(spec, item.path("properties").path("locationInfo"));

    assertThat(propertyNamesOf(locationInfo))
        .containsExactlyInAnyOrder(
            "locationName",
            "zipCode",
            "address",
            "detailAddress",
            "category",
            "mapUrl",
            "latitude",
            "longitude");
  }

  private JsonNode resolve(JsonNode spec, JsonNode schema) {
    String ref = schema.path("$ref").asText();
    return ref.startsWith("#/") ? spec.at(ref.substring(1)) : schema;
  }
}
