package app.bottlenote.global.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
@DisplayName("[integration] 둘러보기 keyword OpenAPI 계약")
class ExploreKeywordOpenApiContractIntegrationTest extends OpenApiSpecTestSupport {

  @Test
  @DisplayName("위스키와 리뷰 둘러보기는 단일 keyword만 문서화한다")
  void 둘러보기는_단일_keyword만_문서화한다() {
    JsonNode spec = fetchSpec();

    assertSingleKeyword(spec, "/api/v1/alcohols/explore/standard");
    assertSingleKeyword(spec, "/api/v1/reviews/explore/standard");
  }

  private void assertSingleKeyword(JsonNode spec, String path) {
    JsonNode parameters = spec.path("paths").path(path).path("get").path("parameters");
    List<String> names =
        StreamSupport.stream(parameters.spliterator(), false)
            .map(parameter -> parameter.path("name").asText())
            .toList();

    assertThat(names).contains("keyword").doesNotContain("keywords");
  }
}
