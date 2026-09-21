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
            "GET /api/v1/mfds/alcohols/category",
            "GET /api/v1/mfds/importers",
            "GET /api/v1/mfds/importers/{importerId}",
            "GET /api/v1/mfds/countries");
  }

  @Test
  @DisplayName("공개 수입 주류 목록은 alcoholCategoryKo 문자열 필터를 문서화한다")
  void 공개_수입_주류_목록은_카테고리_문자열_필터를_문서화한다() {
    var spec = fetchSpec();
    var parameters = spec.at("/paths/~1api~1v1~1mfds~1alcohols/get/parameters");
    var requestSchema =
        java.util.stream.StreamSupport.stream(parameters.spliterator(), false)
            .filter(node -> "request".equals(node.path("name").asText()))
            .map(node -> resolve(spec, node.path("schema")))
            .findFirst()
            .orElseThrow(() -> new AssertionError("목록 검색 request 파라미터가 없습니다"));
    var propertyNames = propertyNamesOf(requestSchema);

    assertThat(propertyNames).contains("alcoholCategoryKo").doesNotContain("alcoholType");
    assertThat(requestSchema.at("/properties/alcoholCategoryKo/type").asText()).isEqualTo("string");
  }

  @Test
  @DisplayName("공개 카테고리 목록은 ko/en/count를 문서화한다")
  void 공개_카테고리_목록은_ko_en_count를_문서화한다() {
    var schema = fetchSpec().at("/components/schemas/MfdsPublicAlcoholCategoryItem");
    assertThat(propertyNamesOf(schema))
        .contains("alcoholCategoryKo", "alcoholCategoryEn", "count");
  }

  @Test
  @DisplayName("공개 수입 주류 목록은 카테고리 영문·숙성연도·연결 boolean을 문서화한다")
  void 공개_수입_주류_목록은_카테고리_숙성_연결상태를_문서화한다() {
    var listSchema = fetchSpec().at("/components/schemas/MfdsPublicAlcoholListItem");
    var requiredNames =
        java.util.stream.StreamSupport.stream(listSchema.path("required").spliterator(), false)
            .map(n -> n.asText())
            .toList();

    assertThat(propertyNamesOf(listSchema))
        .contains(
            "alcoholCategoryKo",
            "alcoholCategoryEn",
            "ageYears",
            "distilleryLinked",
            "regionLinked");
    assertThat(listSchema.at("/properties/distilleryLinked/type").asText()).isEqualTo("boolean");
    assertThat(listSchema.at("/properties/regionLinked/type").asText()).isEqualTo("boolean");
    assertThat(requiredNames)
        .doesNotContain("alcoholCategoryEn", "ageYears")
        .contains("distilleryLinked", "regionLinked");
  }
}
