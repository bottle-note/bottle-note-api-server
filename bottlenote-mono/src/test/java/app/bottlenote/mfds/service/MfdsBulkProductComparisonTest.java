package app.bottlenote.mfds.service;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.alcohols.facade.payload.AlcoholMatchTargetItem;
import app.bottlenote.alcohols.fixture.FakeAlcoholMatchTargetFacade;
import app.bottlenote.mfds.constant.MfdsNormalizationStatus;
import app.bottlenote.mfds.domain.MfdsDeclaration;
import app.bottlenote.mfds.domain.MfdsMatchingSelection;
import app.bottlenote.mfds.dto.request.MfdsBulkMatchingPreviewRequest;
import app.bottlenote.mfds.dto.response.MfdsBulkMatchingPreviewItem;
import app.bottlenote.mfds.dto.response.MfdsBulkMatchingReason;
import app.bottlenote.mfds.fixture.InMemoryMfdsBulkPreviewIssuanceStore;
import app.bottlenote.mfds.fixture.InMemoryMfdsDeclarationRepository;
import app.bottlenote.mfds.fixture.InMemoryMfdsMatchingRepository;
import app.bottlenote.mfds.fixture.InMemoryMfdsMatchingSelectionRepository;
import app.bottlenote.mfds.fixture.MfdsTestData;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("일괄 매칭 제품 비교 30종")
class MfdsBulkProductComparisonTest {

  private static final long ALCOHOL_ID = 10L;
  private static final long ADMIN_ID = 42L;

  private InMemoryMfdsDeclarationRepository declarations;
  private InMemoryMfdsMatchingSelectionRepository selections;
  private FakeAlcoholMatchTargetFacade alcohols;
  private MfdsBulkMatchingService service;
  private int sequence;

  @BeforeEach
  void setUp() {
    declarations = new InMemoryMfdsDeclarationRepository();
    selections = new InMemoryMfdsMatchingSelectionRepository();
    alcohols = new FakeAlcoholMatchTargetFacade();
    alcohols.addAlcohol(
        new AlcoholMatchTargetItem(
            ALCOHOL_ID,
            "기준 주류",
            "Reference",
            null,
            null,
            "브랜디",
            "Brandy",
            4L,
            null,
            null,
            3L,
            null,
            null,
            null,
            null));
    service =
        new MfdsBulkMatchingService(
            declarations,
            alcohols,
            selections,
            new MfdsMatchingHistoryService(
                new InMemoryMfdsMatchingRepository(),
                new MfdsMatchingEvidenceCodec(new ObjectMapper())),
            new InMemoryMfdsBulkPreviewIssuanceStore(),
            new NoopMfdsBulkLockGate(),
            new NoopMfdsBulkSaveGuard(),
            Clock.fixed(Instant.parse("2026-09-24T00:00:00Z"), ZoneOffset.UTC));
  }

  @Test
  @DisplayName("서로 다른 제품 판단 30종 이상을 서비스로 실행하고 결과를 남긴다")
  void 제품_비교_30종을_실행한다() throws Exception {
    List<String> records = new ArrayList<>();
    check(
        records,
        "P01",
        "APPLICABLE",
        null,
        (source, target) -> {
          MfdsTestData.set(source, "volumeMl", 700);
          MfdsTestData.set(target, "volumeMl", 200);
        });
    check(
        records,
        "P03",
        "APPLICABLE",
        null,
        (source, target) -> {
          MfdsTestData.set(source, "processedDate", LocalDate.of(2026, 9, 18));
          MfdsTestData.set(target, "processedDate", LocalDate.of(2025, 9, 11));
        });
    check(
        records,
        "P04",
        "APPLICABLE",
        null,
        (source, target) -> {
          MfdsTestData.set(source, "importerBaseName", "수입사A");
          MfdsTestData.set(target, "importerBaseName", "수입사B");
        });
    check(
        records,
        "P05",
        "APPLICABLE",
        null,
        (source, target) -> {
          MfdsTestData.set(source, "importerBaseName", "수입사A");
          MfdsTestData.set(target, "importerBaseName", null);
        });
    check(
        records,
        "P07",
        "APPLICABLE",
        null,
        (source, target) -> {
          MfdsTestData.set(source, "normalizationVersion", "mfds-normalization-v4");
          MfdsTestData.set(target, "normalizationVersion", "mfds-normalization-v3");
        });
    check(
        records,
        "P08",
        "EXCLUDED",
        null,
        (source, target) -> MfdsTestData.set(target, "productIdentityKeySha256", key(99)));
    check(
        records,
        "P09",
        "EXCLUDED",
        null,
        (source, target) -> {
          MfdsTestData.set(source, "ageYears", (short) 15);
          MfdsTestData.set(target, "ageYears", (short) 12);
          MfdsTestData.set(target, "productIdentityKeySha256", key(99));
        });
    check(
        records,
        "P10",
        "EXCLUDED",
        null,
        (source, target) -> {
          MfdsTestData.set(target, "strengthType", "CASK STRENGTH");
          MfdsTestData.set(target, "productIdentityKeySha256", key(99));
        });
    check(
        records,
        "P18",
        "NEEDS_REVIEW",
        "BATCH_DIFFERS",
        (source, target) -> {
          MfdsTestData.set(source, "batchNumber", "7");
          MfdsTestData.set(target, "batchNumber", "8");
        });
    check(
        records,
        "P19",
        "NEEDS_REVIEW",
        "BATCH_MISSING_ON_SOURCE",
        (source, target) -> MfdsTestData.set(target, "batchNumber", "11"));
    check(
        records,
        "P20",
        "NEEDS_REVIEW",
        "BATCH_MISSING_ON_TARGET",
        (source, target) -> MfdsTestData.set(source, "batchNumber", "4"));
    check(
        records,
        "P22",
        "NEEDS_REVIEW",
        "CASK_DIFFERS",
        (source, target) -> {
          MfdsTestData.set(source, "caskNumber", "10");
          MfdsTestData.set(target, "caskNumber", "11");
        });
    check(
        records,
        "P23",
        "NEEDS_REVIEW",
        "VERSION_MISSING_ON_SOURCE",
        (source, target) -> MfdsTestData.set(target, "versionMarker", "구형"));
    check(
        records,
        "P24",
        "NEEDS_REVIEW",
        "NORMALIZATION_REVIEW_REQUIRED",
        (source, target) ->
            MfdsTestData.set(
                target, "normalizationStatus", MfdsNormalizationStatus.REVIEW_REQUIRED));
    check(
        records,
        "P25",
        "NEEDS_REVIEW",
        "GENERIC_PRODUCT_NAME",
        (source, target) -> {
          MfdsTestData.set(target, "normalizationStatus", MfdsNormalizationStatus.REVIEW_REQUIRED);
          MfdsTestData.set(
              target, "normalizationReasons", List.of("GENERIC_PRODUCT_NAME_REVIEW_REQUIRED"));
        });
    check(
        records,
        "P26",
        "NEEDS_REVIEW",
        "COUNTRY_DIFFERS",
        (source, target) -> {
          MfdsTestData.set(source, "manufactureCountryAlpha2", "IE");
          MfdsTestData.set(target, "manufactureCountryAlpha2", "GB");
        });
    check(
        records,
        "P27",
        "APPLICABLE",
        null,
        (source, target) -> {
          MfdsTestData.set(source, "alcoholCategoryEn", "Brandy");
          MfdsTestData.set(target, "alcoholCategoryEn", "Brandy");
        });
    check(
        records,
        "P28",
        "APPLICABLE",
        null,
        (source, target) -> MfdsTestData.set(source, "volumeMl", 700));
    check(
        records,
        "P29",
        "NEEDS_REVIEW",
        "STRENGTH_DIFFERS",
        (source, target) -> {
          MfdsTestData.set(target, "variantMarkerType", "STRENGTH_ABBREVIATION");
          MfdsTestData.set(target, "variantMarkerRaw", "CS");
        });
    check(
        records,
        "P30",
        "NEEDS_REVIEW",
        "VINTAGE_DIFFERS",
        (source, target) -> {
          MfdsTestData.set(source, "vintageYear", (short) 2010);
          MfdsTestData.set(target, "vintageYear", (short) 2012);
        });
    check(
        records,
        "P31",
        "NEEDS_REVIEW",
        "EDITION_DIFFERS",
        (source, target) -> {
          MfdsTestData.set(source, "editionName", "edition 1");
          MfdsTestData.set(target, "editionName", "edition 2");
        });
    check(
        records,
        "P32",
        "NEEDS_REVIEW",
        "VARIANT_MISSING_ON_SOURCE",
        (source, target) -> {
          MfdsTestData.set(target, "variantMarkerType", "UNKNOWN");
          MfdsTestData.set(target, "variantMarkerRaw", "special");
        });
    check(
        records,
        "P33",
        "NEEDS_REVIEW",
        "AGE_DIFFERS",
        (source, target) -> {
          MfdsTestData.set(source, "skuDisplayNameEn", "Glenfiddich 12 years");
          MfdsTestData.set(target, "skuDisplayNameEn", "Glenfiddich 12 years");
          MfdsTestData.set(source, "ageYears", (short) 12);
          MfdsTestData.set(target, "ageYears", (short) 15);
        });
    check(
        records,
        "P34",
        "EXCLUDED",
        null,
        (source, target) -> MfdsTestData.set(target, "productIdentityKeySha256", null));
    check(
        records,
        "P35",
        "NEEDS_REVIEW",
        "ADMIN_RELEASED",
        (source, target) ->
            selections.save(
                MfdsMatchingSelection.adminRevoke(
                    target.getId(),
                    "ALCOHOL",
                    1L,
                    "ADMIN_RELEASE",
                    ADMIN_ID,
                    LocalDateTime.of(2026, 9, 24, 0, 0))));
    check(
        records,
        "ABV-DIFF",
        "NEEDS_REVIEW",
        "ABV_DIFFERS",
        (source, target) -> {
          MfdsTestData.set(source, "abvPercent", new java.math.BigDecimal("40.000"));
          MfdsTestData.set(target, "abvPercent", new java.math.BigDecimal("43.000"));
        });
    check(
        records,
        "ABV-ONE",
        "NEEDS_REVIEW",
        "ABV_MISSING_ON_TARGET",
        (source, target) ->
            MfdsTestData.set(source, "abvPercent", new java.math.BigDecimal("40.000")));
    check(
        records,
        "STRENGTH-TYPE",
        "NEEDS_REVIEW",
        "STRENGTH_DIFFERS",
        (source, target) -> {
          MfdsTestData.set(source, "strengthType", "STANDARD");
          MfdsTestData.set(target, "strengthType", "CASK STRENGTH");
        });
    check(
        records,
        "CASK-ONE",
        "NEEDS_REVIEW",
        "CASK_MISSING_ON_SOURCE",
        (source, target) -> MfdsTestData.set(target, "caskNumber", "20"));
    check(
        records,
        "EDITION-ONE",
        "NEEDS_REVIEW",
        "EDITION_MISSING_ON_TARGET",
        (source, target) -> MfdsTestData.set(source, "editionName", "edition 4"));
    check(
        records,
        "AGE-TEXT",
        "NEEDS_REVIEW",
        "AGE_TEXT_CONFLICT",
        (source, target) -> {
          MfdsTestData.set(target, "skuDisplayNameKo", "글렌피딕 12년");
          MfdsTestData.set(target, "skuDisplayNameEn", "Glenfiddich 15 years");
        });
    check(
        records,
        "AGE-STORED",
        "NEEDS_REVIEW",
        "AGE_STORED_MISMATCH",
        (source, target) -> {
          MfdsTestData.set(target, "skuDisplayNameEn", "Glenfiddich 12 years");
          MfdsTestData.set(target, "ageYears", (short) 15);
        });
    check(
        records,
        "VINTAGE-ONE",
        "NEEDS_REVIEW",
        "VINTAGE_MISSING_ON_TARGET",
        (source, target) -> MfdsTestData.set(source, "vintageYear", (short) 1998));
    assertThat(records).hasSizeGreaterThanOrEqualTo(30);
    Files.writeString(
        Path.of("/tmp/mfds-bulk-product-comparison.json"), String.join("\n", records));
  }

  private void check(
      List<String> records, String id, String expectedClass, String expectedReason, Setup setup) {
    byte[] identity = key(++sequence);
    MfdsDeclaration source = row("S-" + id, identity);
    MfdsDeclaration target = row("T-" + id, identity);
    setup.apply(source, target);
    var preview =
        service.preview(
            new MfdsBulkMatchingPreviewRequest(source.getId(), ALCOHOL_ID, null, null), ADMIN_ID);
    boolean present =
        preview.items().stream().anyMatch(item -> target.getId().equals(item.declarationId()));
    String actualClass = present ? item(preview, target.getId()).classification() : "EXCLUDED";
    String actualReasons =
        present
            ? item(preview, target.getId()).reasons().stream()
                .map(MfdsBulkMatchingReason::code)
                .toList()
                .toString()
            : "[]";
    records.add(
        id
            + " synthetic expected="
            + expectedClass
            + " actual="
            + actualClass
            + " reasons="
            + actualReasons);
    assertThat(actualClass).as(id).isEqualTo(expectedClass);
    if (expectedReason != null) {
      assertThat(actualReasons).as(id).contains(expectedReason);
    }
  }

  private MfdsBulkMatchingPreviewItem item(
      app.bottlenote.mfds.dto.response.MfdsBulkMatchingPreviewResponse preview, Long id) {
    return preview.items().stream()
        .filter(item -> id.equals(item.declarationId()))
        .findFirst()
        .orElseThrow();
  }

  private MfdsDeclaration row(String rcno, byte[] identity) {
    MfdsDeclaration declaration =
        MfdsTestData.declaration(
            rcno, MfdsNormalizationStatus.NORMALIZED, null, null, null, "몽키숄더", "monkey shoulder");
    MfdsTestData.set(declaration, "productIdentityKeySha256", identity);
    return declarations.save(declaration);
  }

  private static byte[] key(int marker) {
    byte[] value = new byte[32];
    value[31] = (byte) marker;
    return value;
  }

  @FunctionalInterface
  interface Setup {
    void apply(MfdsDeclaration source, MfdsDeclaration target);
  }
}
