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

  @Test
  @DisplayName("알코올 상세는 공개 MFDS 신고 목록과 Admin 공개 필드 중첩을 문서화하고 내부 필드는 제외한다")
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
    JsonNode declarations =
        resolve(
            spec,
            declarationsSchema.has("items")
                ? declarationsSchema.path("items")
                : declarationsSchema);
    List<String> declarationFields = propertyNamesOf(declarations);
    assertThat(declarationFields)
        .contains(
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
            "importer")
        .doesNotContain(
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
            "matchedAt");

    JsonNode importer = resolve(spec, declarations.path("properties").path("importer"));
    assertThat(propertyNamesOf(importer))
        .contains(
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
            "description")
        .doesNotContain("adminNote", "adminStatus", "reviewedBy", "reviewedAt");
  }

  private JsonNode resolve(JsonNode spec, JsonNode schema) {
    String ref = schema.path("$ref").asText();
    return ref.startsWith("#/") ? spec.at(ref.substring(1)) : schema;
  }
}
