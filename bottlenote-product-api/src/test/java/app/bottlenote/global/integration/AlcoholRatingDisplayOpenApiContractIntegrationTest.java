package app.bottlenote.global.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
@DisplayName("[integration] 알코올 별점 노출 OpenAPI 계약")
class AlcoholRatingDisplayOpenApiContractIntegrationTest extends OpenApiSpecTestSupport {

  @Test
  @DisplayName("myAvgRating은 최신 활성 리뷰 1건의 선택 규칙을 문서화한다")
  void myAvgRating은_최신_활성_리뷰_단건_규칙을_문서화한다() {
    JsonNode spec = fetchSpec();

    assertLatestActiveReviewRatingRule(spec, "GET /api/v1/alcohols/{alcoholId}");
    assertLatestActiveReviewRatingRule(spec, "GET /api/v1/alcohols/explore/standard");
  }

  private void assertLatestActiveReviewRatingRule(JsonNode spec, String endpoint) {
    SpecOperation operation =
        operationsOf(spec).stream()
            .filter(candidate -> candidate.endpoint().equals(endpoint))
            .findFirst()
            .orElseThrow();

    assertThat(operation.definition().path("description").asText())
        .contains("ACTIVE 리뷰 중 최신(id 최대) 1건의 별점")
        .contains("없으면 0.0");
  }
}
