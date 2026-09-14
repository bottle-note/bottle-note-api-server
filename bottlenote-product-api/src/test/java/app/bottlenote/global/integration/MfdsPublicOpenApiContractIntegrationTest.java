package app.bottlenote.global.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
@DisplayName("[integration] Product 수입 정보 OpenAPI 계약")
class MfdsPublicOpenApiContractIntegrationTest extends OpenApiSpecTestSupport {

  @Test
  @DisplayName("수입 정보 태그에 공개 조회 경로가 있다")
  void 수입_정보_태그에_공개_경로가_있다() {
    var endpoints =
        operationsOf(fetchSpec()).stream()
            .filter(operation -> "수입 정보".equals(operation.tag()))
            .map(SpecOperation::endpoint)
            .toList();

    assertThat(endpoints)
        .contains(
            "GET /api/v1/mfds/alcohols",
            "GET /api/v1/mfds/alcohols/{id}",
            "GET /api/v1/mfds/importers",
            "GET /api/v1/mfds/importers/{importerId}",
            "GET /api/v1/mfds/countries");
  }
}
