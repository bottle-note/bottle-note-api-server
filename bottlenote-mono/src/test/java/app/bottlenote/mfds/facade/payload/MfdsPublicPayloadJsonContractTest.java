package app.bottlenote.mfds.facade.payload;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.alcohols.dto.response.AlcoholDetailItem;
import app.bottlenote.alcohols.dto.response.AlcoholDetailResponse;
import app.bottlenote.alcohols.dto.response.FriendsDetailResponse;
import app.bottlenote.review.dto.response.ReviewListResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("Product MFDS 공개 payload JSON 계약")
class MfdsPublicPayloadJsonContractTest {

  private static final ObjectMapper MAPPER =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

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

  private static final Set<String> FORBIDDEN_FIELDS =
      Set.of(
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
  @DisplayName("상세 JSON의 mfdsDeclarations는 항상 배열이며 공개 필드 exact allowlist만 포함한다")
  void 상세_JSON_공개_필드를_exact_allowlist로_고정한다() throws Exception {
    MfdsPublicImporterItem importer =
        new MfdsPublicImporterItem(
            9L,
            "BIZ-001",
            "제1호",
            "보틀상사",
            "김대표",
            LocalDate.of(2020, 1, 2),
            "식약처",
            "서울",
            "02-0000-0000",
            "주류",
            "영업",
            "공개 설명");
    MfdsPublicDeclarationItem declaration =
        new MfdsPublicDeclarationItem(
            1L,
            "RCNO-001",
            "글렌피딕",
            "Glenfiddich",
            "12년",
            "12yo",
            700,
            700,
            1,
            new BigDecimal("40.0"),
            (short) 12,
            null,
            "Edition",
            "Cask-1",
            "Batch-1",
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2027, 1, 1),
            "보틀상사",
            "증류소",
            "위스키",
            "Whisky",
            "위스키",
            "Whisky",
            "스코틀랜드",
            "영국",
            importer);

    AlcoholDetailResponse response =
        AlcoholDetailResponse.builder()
            .alcohols(AlcoholDetailItem.builder().alcoholId(42L).build())
            .friendsInfo(FriendsDetailResponse.of(0L, List.of()))
            .reviewInfo(ReviewListResponse.of(List.of()))
            .mfdsDeclarations(List.of(declaration))
            .build();

    JsonNode root = MAPPER.readTree(MAPPER.writeValueAsString(response));
    JsonNode declarations = root.path("mfdsDeclarations");
    assertThat(declarations.isArray()).isTrue();
    assertThat(declarations).hasSize(1);

    JsonNode item = declarations.get(0);
    assertThat(fieldNames(item)).containsExactlyInAnyOrderElementsOf(DECLARATION_FIELDS);
    assertThat(fieldNames(item)).doesNotContainAnyElementsOf(FORBIDDEN_FIELDS);

    JsonNode importerNode = item.path("importer");
    assertThat(importerNode.isObject()).isTrue();
    assertThat(fieldNames(importerNode)).containsExactlyInAnyOrderElementsOf(IMPORTER_FIELDS);
    assertThat(fieldNames(importerNode)).doesNotContainAnyElementsOf(FORBIDDEN_FIELDS);
  }

  @Test
  @DisplayName("mfdsDeclarations는 null 입력이어도 빈 배열이고 importer는 null 가능하다")
  void 빈_배열과_nullable_importer를_직렬화한다() throws Exception {
    MfdsPublicDeclarationItem withoutImporter =
        new MfdsPublicDeclarationItem(
            2L,
            "RCNO-002",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);

    AlcoholDetailResponse empty =
        AlcoholDetailResponse.builder()
            .alcohols(AlcoholDetailItem.builder().alcoholId(1L).build())
            .friendsInfo(FriendsDetailResponse.of(0L, List.of()))
            .reviewInfo(ReviewListResponse.of(List.of()))
            .mfdsDeclarations(null)
            .build();
    AlcoholDetailResponse withNullImporter =
        AlcoholDetailResponse.builder()
            .alcohols(AlcoholDetailItem.builder().alcoholId(1L).build())
            .friendsInfo(FriendsDetailResponse.of(0L, List.of()))
            .reviewInfo(ReviewListResponse.of(List.of()))
            .mfdsDeclarations(List.of(withoutImporter))
            .build();

    JsonNode emptyNode = MAPPER.readTree(MAPPER.writeValueAsString(empty));
    assertThat(emptyNode.path("mfdsDeclarations").isArray()).isTrue();
    assertThat(emptyNode.path("mfdsDeclarations")).isEmpty();

    JsonNode item =
        MAPPER.readTree(MAPPER.writeValueAsString(withNullImporter)).path("mfdsDeclarations").get(0);
    assertThat(item.path("importer").isNull()).isTrue();
    assertThat(fieldNames(item)).containsExactlyInAnyOrderElementsOf(DECLARATION_FIELDS);
  }

  private static List<String> fieldNames(JsonNode node) {
    List<String> names = new ArrayList<>();
    node.fieldNames().forEachRemaining(names::add);
    return names;
  }
}
