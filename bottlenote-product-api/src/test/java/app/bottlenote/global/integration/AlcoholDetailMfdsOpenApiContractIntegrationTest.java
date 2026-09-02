package app.bottlenote.global.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
@DisplayName("[integration] Product 알코올 상세 MFDS OpenAPI 계약")
class AlcoholDetailMfdsOpenApiContractIntegrationTest extends OpenApiSpecTestSupport {

  private static final List<String> DECLARATION_FIELDS =
      List.of(
          "id",
          "rcno",
          "baseProductNameKo",
          "baseProductNameEn",
          "skuDisplayNameKo",
          "skuDisplayNameEn",
          "volumeMl",
          "unitVolumeMl",
          "packageCount",
          "abvPercent",
          "ageYears",
          "vintageYear",
          "editionName",
          "caskNumber",
          "batchNumber",
          "expiryStart",
          "expiryEnd",
          "importerBaseName",
          "manufacturerName",
          "alcoholNameKo",
          "alcoholNameEn",
          "alcoholCategoryKo",
          "alcoholCategoryEn",
          "manufactureCountryNameKo",
          "exportCountryNameKo",
          "importer");

  private static final List<String> IMPORTER_FIELDS =
      List.of(
          "id",
          "officialBusinessCode",
          "licenseNo",
          "businessName",
          "representativeName",
          "permitDate",
          "institutionName",
          "primaryAddress",
          "telephoneNo",
          "industryName",
          "operatingStatus",
          "description");

  private static final List<String> FORBIDDEN_FIELDS =
      List.of(
          "volumeRaw",
          "abvRaw",
          "normalizationStatus",
          "normalizationReasons",
          "unparsedFragments",
          "reviewStatus",
          "reviewedBy",
          "reviewedAt",
          "reviewNote",
          "importerLinkSource",
          "selectedAlcoholId",
          "alcoholMatchDecision",
          "alcoholCandidates",
          "distilleryCandidates",
          "regionCandidates",
          "matchedAt",
          "adminNote",
          "adminStatus");

  @Test
  @DisplayName("알코올 상세는 공개 MFDS 신고 목록 exact allowlist와 빈 배열·nullable importer 계약을 문서화한다")
  void 알코올_상세_MFDS_공개_계약을_문서화한다() {
    JsonNode spec = fetchSpec();
    SpecOperation operation =
        operationsOf(spec).stream()
            .filter(candidate -> candidate.endpoint().equals("GET /api/v1/alcohols/{alcoholId}"))
            .findFirst()
            .orElseThrow();

    assertThat(operation.definition().path("description").asText()).contains("MFDS");

    JsonNode successSchema = resolve(spec, operation.successSchema());
    JsonNode detail =
        successSchema.path("properties").has("data")
            ? resolve(spec, successSchema.path("properties").path("data"))
            : successSchema;
    assertThat(propertyNamesOf(detail))
        .contains("alcohols", "friendsInfo", "reviewInfo", "mfdsDeclarations");

    JsonNode declarationsSchema = detail.path("properties").path("mfdsDeclarations");
    assertThat(declarationsSchema.path("type").asText()).isEqualTo("array");
    assertThat(declarationsSchema.path("nullable").asBoolean(false)).isFalse();

    JsonNode declarations =
        resolve(
            spec,
            declarationsSchema.has("items")
                ? declarationsSchema.path("items")
                : declarationsSchema);
    List<String> declarationFields = propertyNamesOf(declarations);
    assertThat(declarationFields).containsExactlyInAnyOrderElementsOf(DECLARATION_FIELDS);
    assertThat(declarationFields).doesNotContainAnyElementsOf(FORBIDDEN_FIELDS);

    JsonNode importerSchema = declarations.path("properties").path("importer");
    JsonNode importer = resolve(spec, importerSchema);
    assertThat(allowsNull(importerSchema))
        .as("연결 없는 신고의 importer는 OpenAPI에서도 null을 허용해야 한다")
        .isTrue();
    List<String> importerFields = propertyNamesOf(importer);
    assertThat(importerFields).containsExactlyInAnyOrderElementsOf(IMPORTER_FIELDS);
    assertThat(importerFields).doesNotContainAnyElementsOf(FORBIDDEN_FIELDS);
  }

  private JsonNode resolve(JsonNode spec, JsonNode schema) {
    String ref = schema.path("$ref").asText();
    return ref.startsWith("#/") ? spec.at(ref.substring(1)) : schema;
  }

  private boolean allowsNull(JsonNode schema) {
    if (schema.path("nullable").asBoolean(false)) {
      return true;
    }
    if (schema.path("type").isArray()) {
      for (JsonNode type : schema.path("type")) {
        if ("null".equals(type.asText())) {
          return true;
        }
      }
    }
    for (String composition : List.of("anyOf", "oneOf")) {
      for (JsonNode candidate : schema.path(composition)) {
        if ("null".equals(candidate.path("type").asText())) {
          return true;
        }
      }
    }
    return false;
  }
}
