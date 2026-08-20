package app.bottlenote.global.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
@DisplayName("[integration] Product curation 선택 정렬 OpenAPI 계약")
class CurationSortOpenApiContractIntegrationTest extends OpenApiSpecTestSupport {

  @Test
  @DisplayName("feed는 선택 정렬 query enum과 기본값을 명시한다")
  void feed_exposesSelectableSortParameterSchemas() {
    SpecOperation operation =
        operationsOf(fetchSpec()).stream()
            .filter(candidate -> candidate.endpoint().equals("GET /api/v2/curations/feed"))
            .findFirst()
            .orElseThrow();

    assertSortParameter(operation, "sortType", List.of("EXPOSURE_START_DATE", "DISPLAY_ORDER"), "EXPOSURE_START_DATE");
    assertSortParameter(operation, "sortOrder", List.of("ASC", "DESC"), "DESC");
  }

  private void assertSortParameter(
      SpecOperation operation, String name, List<String> expectedEnum, String expectedDefault) {
    JsonNode schema =
        StreamSupport.stream(operation.definition().path("parameters").spliterator(), false)
            .filter(parameter -> parameter.path("name").asText().equals(name))
            .findFirst()
            .orElseThrow()
            .path("schema");

    assertThat(schema.path("type").asText()).isEqualTo("string");
    assertThat(StreamSupport.stream(schema.path("enum").spliterator(), false).map(JsonNode::asText).toList())
        .containsExactlyInAnyOrderElementsOf(expectedEnum);
    assertThat(schema.path("default").asText()).isEqualTo(expectedDefault);
  }
}
