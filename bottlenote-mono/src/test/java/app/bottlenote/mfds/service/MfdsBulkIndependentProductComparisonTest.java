package app.bottlenote.mfds.service;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.alcohols.facade.payload.AlcoholMatchTargetItem;
import app.bottlenote.alcohols.facade.payload.DistilleryMatchTargetItem;
import app.bottlenote.alcohols.facade.payload.RegionMatchTargetItem;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("일괄 매칭 독립 제품 비교")
class MfdsBulkIndependentProductComparisonTest {

  private static final long ADMIN_ID = 42L;
  private static final String CASES = "/mfds/mfds-bulk-independent-product-cases.json";
  private static final Pattern KEY_MARKER = Pattern.compile("key\\((\\d+)\\)");
  private static final Set<String> QA_PROPOSALS = Set.of("IP07", "IP08", "IP38");

  private final ObjectMapper mapper = new ObjectMapper();
  private FakeAlcoholMatchTargetFacade alcohols;

  @BeforeEach
  void setUp() {
    alcohols = new FakeAlcoholMatchTargetFacade();
    alcohols.addAlcohol(alcohol(9001L, "합성 위스키", "Synthetic Whisky", 1L, 1L));
    alcohols.addAlcohol(alcohol(9002L, "합성 다른 위스키", "Other Synthetic Whisky", 2L, 2L));
    alcohols.addAlcohol(alcohol(9003L, "합성 위스키", "Synthetic Whisky", 1L, 1L));
    alcohols.addAlcohol(alcohol(9101L, "헤네시 VS 격리", "Hennessy VS Isolated", 3L, 3L));
    alcohols.addAlcohol(alcohol(9102L, "보드카 격리", "Vodka Isolated", 4L, 4L));
    alcohols.addAlcohol(alcohol(4734L, "(격리 복제) 주류 4734", "(isolated) alcohol 4734", 0L, 3L));
    for (long id : List.of(1L, 2L, 3L, 4L, 7L, 12L)) {
      alcohols.addDistillery(new DistilleryMatchTargetItem(id, "증류소 " + id, "Distillery " + id));
    }
    for (long id : List.of(1L, 2L, 3L, 4L, 16L, 19L)) {
      alcohols.addRegion(new RegionMatchTargetItem(id, "지역 " + id, "Region " + id));
    }
  }

  @Test
  @DisplayName("독립 제품 사례를 실제 미리보기 서비스로 실행하고 기대값과 대조한다")
  void 독립_제품_사례를_서비스로_실행한다() throws Exception {
    JsonNode cases;
    try (InputStream input =
        MfdsBulkIndependentProductComparisonTest.class.getResourceAsStream(CASES)) {
      if (input == null) {
        throw new IllegalStateException(CASES);
      }
      cases = mapper.readTree(input).get("cases");
    }
    List<Map<String, Object>> records = new ArrayList<>();
    List<String> failures = new ArrayList<>();
    for (JsonNode item : cases) {
      Map<String, Object> record = execute(item);
      records.add(record);
      if (!"PASS".equals(record.get("verdict"))) {
        failures.add(record.get("id") + " " + record.get("detail"));
      }
    }
    List<String> lines = new ArrayList<>();
    for (Map<String, Object> record : records) {
      lines.add(mapper.writeValueAsString(record));
    }
    Path result = Path.of("build", "reports", "mfds-bulk-independent-product-comparison.json");
    Files.createDirectories(result.getParent());
    Files.writeString(result, String.join("\n", lines) + "\n");
    long strictPass =
        records.stream()
            .filter(record -> !"LINKAGE_STATE".equals(record.get("category")))
            .filter(record -> "PASS".equals(record.get("verdict")))
            .count();
    long strictWithoutProposal =
        records.stream()
            .filter(record -> !"LINKAGE_STATE".equals(record.get("category")))
            .filter(record -> !QA_PROPOSALS.contains(record.get("id")))
            .filter(record -> "PASS".equals(record.get("verdict")))
            .count();
    assertThat(records).hasSize(49);
    assertThat(strictPass).isGreaterThanOrEqualTo(30);
    assertThat(strictWithoutProposal).isGreaterThanOrEqualTo(30);
    assertThat(failures).isEmpty();
  }

  private Map<String, Object> execute(JsonNode item) {
    String id = item.get("id").asText();
    String expectedClass = item.get("expected").get("class").asText();
    String reasonsMode = item.get("expected").get("reasonsMode").asText();
    List<String> expectedReasons = texts(item.get("expected").get("reasons"));
    Map<String, Object> record = baseRecord(item, expectedClass, expectedReasons, reasonsMode);
    try {
      Outcome outcome = preview(item);
      record.put("actualClass", outcome.classification());
      record.put("actualReasons", outcome.reasons());
      boolean pass =
          matches(expectedClass, expectedReasons, reasonsMode, outcome) && specificAge(id, outcome);
      record.put("verdict", pass ? "PASS" : "FAIL");
      record.put("detail", pass ? "" : "expected " + expectedClass + " " + expectedReasons);
    } catch (RuntimeException exception) {
      record.put("actualClass", "ERROR");
      record.put("actualReasons", List.of(exception.getClass().getSimpleName()));
      record.put("verdict", "FAIL");
      record.put("detail", exception.getMessage());
    }
    return record;
  }

  @Test
  @DisplayName("저장 연수가 없어도 표시명 숙성의 차이와 한쪽 결측은 확인 필요가 된다")
  void 표시명_숙성만_달라도_확인이_필요하다() {
    assertDisplayAge(
        "RR1-DIFF", "Glenfiddich 12 years", "Glenfiddich 15 years", "NEEDS_REVIEW", "AGE_DIFFERS");
    assertDisplayAge(
        "RR1-SOURCE",
        "Glenfiddich",
        "Glenfiddich 15 years",
        "NEEDS_REVIEW",
        "AGE_MISSING_ON_SOURCE");
    assertDisplayAge(
        "RR1-TARGET",
        "Glenfiddich 15 years",
        "Glenfiddich",
        "NEEDS_REVIEW",
        "AGE_MISSING_ON_TARGET");
    assertDisplayAge(
        "RR1-SAME", "Glenfiddich 15 years", "Glenfiddich 15 years", "APPLICABLE", null);
    assertDisplayAge(
        "QX01", "Monkey Shoulder 12yr", "Monkey Shoulder 15yr", "NEEDS_REVIEW", "AGE_DIFFERS");
    assertDisplayAge(
        "YR-SPACE",
        "Monkey Shoulder 12 yr",
        "Monkey Shoulder 15 YRS",
        "NEEDS_REVIEW",
        "AGE_DIFFERS");
    assertDisplayAge(
        "YR-SAME", "Monkey Shoulder 12YR", "Monkey Shoulder 12yrs", "APPLICABLE", null);
  }

  private void assertDisplayAge(
      String id,
      String sourceName,
      String targetName,
      String expectedClass,
      String expectedReason) {
    InMemoryMfdsDeclarationRepository declarations = new InMemoryMfdsDeclarationRepository();
    MfdsBulkMatchingService bulk =
        service(declarations, new InMemoryMfdsMatchingSelectionRepository());
    MfdsDeclaration source = named(declarations, "S-" + id, sourceName, 210);
    MfdsDeclaration target = named(declarations, "T-" + id, targetName, 210);
    Outcome outcome =
        bulk
            .preview(
                new MfdsBulkMatchingPreviewRequest(source.getId(), 9001L, null, null), ADMIN_ID)
            .items()
            .stream()
            .filter(row -> target.getId().equals(row.declarationId()))
            .findFirst()
            .map(MfdsBulkIndependentProductComparisonTest::outcome)
            .orElseThrow();
    assertThat(outcome.classification()).as(id).isEqualTo(expectedClass);
    if (expectedReason == null) {
      assertThat(outcome.reasons()).as(id).isEmpty();
    } else {
      assertThat(outcome.reasons()).as(id).contains(expectedReason);
    }
  }

  private MfdsDeclaration named(
      InMemoryMfdsDeclarationRepository declarations, String rcno, String englishName, int marker) {
    MfdsDeclaration declaration =
        MfdsTestData.declaration(
            rcno, MfdsNormalizationStatus.NORMALIZED, null, null, null, "몽키숄더", "monkey shoulder");
    MfdsTestData.set(declaration, "productIdentityKeySha256", key(marker));
    MfdsTestData.set(declaration, "skuDisplayNameEn", englishName);
    return declarations.save(declaration);
  }

  private Outcome preview(JsonNode item) {
    InMemoryMfdsDeclarationRepository declarations = new InMemoryMfdsDeclarationRepository();
    InMemoryMfdsMatchingSelectionRepository selections =
        new InMemoryMfdsMatchingSelectionRepository();
    MfdsBulkMatchingService service = service(declarations, selections);
    MfdsDeclaration source =
        save(declarations, "S-" + item.get("id").asText(), item.get("source"), item);
    MfdsDeclaration target =
        save(declarations, "T-" + item.get("id").asText(), item.get("target"), item);
    recordHistory(selections, target.getId(), item.get("target"));
    JsonNode request = item.get("request");
    var preview =
        service.preview(
            new MfdsBulkMatchingPreviewRequest(
                source.getId(),
                request.get("alcoholId").asLong(),
                nullableLong(request, "distilleryId"),
                nullableLong(request, "regionId")),
            ADMIN_ID);
    return preview.items().stream()
        .filter(row -> target.getId().equals(row.declarationId()))
        .findFirst()
        .map(MfdsBulkIndependentProductComparisonTest::outcome)
        .orElse(new Outcome("NOT_IN_GROUP", List.of()));
  }

  private MfdsDeclaration save(
      InMemoryMfdsDeclarationRepository declarations, String rcno, JsonNode side, JsonNode item) {
    MfdsDeclaration declaration =
        MfdsTestData.declaration(
            rcno, MfdsNormalizationStatus.NORMALIZED, null, null, null, "몽키숄더", "monkey shoulder");
    MfdsTestData.set(declaration, "productIdentityKeySha256", identityKey(side, item));
    apply(declaration, side.get("set"));
    return declarations.save(declaration);
  }

  private void apply(MfdsDeclaration declaration, JsonNode values) {
    if (values == null) {
      return;
    }
    values
        .fields()
        .forEachRemaining(
            entry ->
                MfdsTestData.set(
                    declaration, entry.getKey(), convert(entry.getKey(), entry.getValue())));
  }

  private void recordHistory(
      InMemoryMfdsMatchingSelectionRepository selections, Long declarationId, JsonNode side) {
    JsonNode history = side.get("selectionHistory");
    if (history == null) {
      return;
    }
    for (JsonNode entry : history) {
      LocalDateTime selectedAt =
          LocalDateTime.of(2026, 9, 24, 0, 0).plusSeconds(entry.get("order").asInt());
      String action = entry.get("action").asText();
      MfdsMatchingSelection selection =
          "REVOKE".equals(action)
              ? MfdsMatchingSelection.adminRevoke(
                  declarationId, "ALCOHOL", 1L, "ADMIN_RELEASE", ADMIN_ID, selectedAt)
              : MfdsMatchingSelection.adminSelect(
                  declarationId, "ALCOHOL", 1L, "ADMIN_SELECT", ADMIN_ID, selectedAt);
      selections.save(selection);
    }
  }

  private MfdsBulkMatchingService service(
      InMemoryMfdsDeclarationRepository declarations,
      InMemoryMfdsMatchingSelectionRepository selections) {
    return new MfdsBulkMatchingService(
        declarations,
        alcohols,
        selections,
        new MfdsMatchingHistoryService(
            new InMemoryMfdsMatchingRepository(), new MfdsMatchingEvidenceCodec(mapper)),
        new InMemoryMfdsBulkPreviewIssuanceStore(),
        new NoopMfdsBulkLockGate(),
        new NoopMfdsBulkSaveGuard(),
        Clock.fixed(Instant.parse("2026-09-24T00:00:00Z"), ZoneOffset.UTC));
  }

  private static boolean specificAge(String id, Outcome outcome) {
    if ("IP32".equals(id)) {
      return outcome.reasons().contains("AGE_TEXT_CONFLICT")
          && outcome.reasons().stream().allMatch("AGE_TEXT_CONFLICT"::equals);
    }
    if ("IP33".equals(id)) {
      return outcome.reasons().contains("AGE_STORED_MISMATCH")
          && outcome.reasons().stream().allMatch("AGE_STORED_MISMATCH"::equals);
    }
    return true;
  }

  private static boolean matches(
      String expectedClass, List<String> expectedReasons, String reasonsMode, Outcome outcome) {
    if (!expectedClass.equals(outcome.classification())) {
      return false;
    }
    List<String> actual = outcome.reasons();
    return switch (reasonsMode) {
      case "EXACT" ->
          Set.copyOf(actual).equals(Set.copyOf(expectedReasons))
              && actual.size() == expectedReasons.size();
      case "CONTAINS" -> actual.containsAll(expectedReasons);
      case "PREFIX" -> actual.stream().anyMatch(code -> code.startsWith("AGE_"));
      case "ANY" -> true;
      default -> false;
    };
  }

  private static Outcome outcome(MfdsBulkMatchingPreviewItem item) {
    return new Outcome(
        item.classification(), item.reasons().stream().map(MfdsBulkMatchingReason::code).toList());
  }

  private Map<String, Object> baseRecord(
      JsonNode item, String expectedClass, List<String> expectedReasons, String reasonsMode) {
    Map<String, Object> record = new LinkedHashMap<>();
    record.put("id", item.get("id").asText());
    record.put("family", item.get("family").asText());
    record.put("category", item.get("category").asText());
    record.put("existingCases", texts(item.get("existingCases")));
    record.put("dataKind", item.get("dataKind").asText());
    record.put("expectedClass", expectedClass);
    record.put("expectedReasons", expectedReasons);
    record.put("reasonsMode", reasonsMode);
    return record;
  }

  private byte[] identityKey(JsonNode side, JsonNode item) {
    if (side.has("identityKey")) {
      JsonNode explicit = side.get("identityKey");
      if (explicit.isNull()) {
        return null;
      }
      Matcher matcher = KEY_MARKER.matcher(explicit.asText());
      if (!matcher.matches()) {
        throw new IllegalArgumentException(explicit.asText());
      }
      return key(Integer.parseInt(matcher.group(1)));
    }
    return key(item.get("keyMarker").asInt());
  }

  private Object convert(String field, JsonNode node) {
    if (node.isNull()) {
      return null;
    }
    return switch (field) {
      case "ageYears", "vintageYear" -> (short) node.asInt();
      case "abvPercent" -> new BigDecimal(node.asText());
      case "processedDate" -> LocalDate.parse(node.asText());
      case "normalizationStatus" -> MfdsNormalizationStatus.valueOf(node.asText());
      case "normalizationReasons" ->
          StreamSupport.stream(node.spliterator(), false).map(JsonNode::asText).toList();
      case "importerId", "selectedAlcoholId", "selectedDistilleryId", "selectedRegionId" ->
          node.asLong();
      case "unitVolumeMl", "volumeMl" -> node.asInt();
      default -> node.asText();
    };
  }

  private static Long nullableLong(JsonNode node, String field) {
    return node.hasNonNull(field) ? node.get(field).asLong() : null;
  }

  private static List<String> texts(JsonNode node) {
    if (node == null || node.isNull()) {
      return List.of();
    }
    return StreamSupport.stream(node.spliterator(), false).map(JsonNode::asText).toList();
  }

  private static AlcoholMatchTargetItem alcohol(
      long id, String korName, String engName, long distilleryId, long regionId) {
    return new AlcoholMatchTargetItem(
        id,
        korName,
        engName,
        null,
        null,
        null,
        null,
        regionId,
        null,
        null,
        distilleryId,
        null,
        null,
        null,
        null);
  }

  private static byte[] key(int marker) {
    byte[] value = new byte[32];
    value[31] = (byte) marker;
    return value;
  }

  private record Outcome(String classification, List<String> reasons) {}
}
