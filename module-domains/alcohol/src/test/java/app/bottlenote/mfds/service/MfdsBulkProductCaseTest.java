package app.bottlenote.mfds.service;

import static app.bottlenote.mfds.service.MfdsBulkTestFixture.ADMIN_ID;
import static app.bottlenote.mfds.service.MfdsBulkTestFixture.BASE_CLOCK;
import static app.bottlenote.mfds.service.MfdsBulkTestFixture.alcohol;
import static app.bottlenote.mfds.service.MfdsBulkTestFixture.item;
import static app.bottlenote.mfds.service.MfdsBulkTestFixture.key;
import static app.bottlenote.mfds.service.MfdsBulkTestFixture.preview;
import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.alcohols.facade.payload.DistilleryMatchTargetItem;
import app.bottlenote.alcohols.facade.payload.RegionMatchTargetItem;
import app.bottlenote.mfds.constant.MfdsNormalizationStatus;
import app.bottlenote.mfds.domain.MfdsDeclaration;
import app.bottlenote.mfds.domain.MfdsMatchingSelection;
import app.bottlenote.mfds.dto.response.MfdsBulkMatchingReasonItem;
import app.bottlenote.mfds.fixture.MfdsTestData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@Tag("unit")
@DisplayName("일괄 매칭 제품 분류 사례")
class MfdsBulkProductCaseTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final JsonNode DATA = load("/mfds/mfds-bulk-product-cases.json");
  private static final Map<String, List<String>> RESULTS = new ConcurrentHashMap<>();

  static Stream<Named<JsonNode>> cases() {
    return scenarios().map(c -> Named.of(c.get("id").asText() + " " + c.get("rule").asText(), c));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("cases")
  @DisplayName("사례별로 원본·대상을 만들어 미리보기할 때 기대 분류와 사유를 반환한다")
  void 사례를_분류할_수_있다(JsonNode scenario) {
    // given
    MfdsBulkTestFixture fixture = baselineFixture();
    String id = scenario.get("id").asText();
    MfdsDeclaration source = fixture.row("S-" + id, key(1));
    MfdsDeclaration target = fixture.row("T-" + id, targetKey(scenario));
    overwrite(source, scenario.get("source"));
    overwrite(target, scenario.get("target"));
    recordHistory(fixture, target.getId(), scenario.get("history"));
    JsonNode request = scenario.has("request") ? scenario.get("request") : baseline("request");

    // when
    var row =
        item(
            preview(
                fixture.service(BASE_CLOCK),
                source.getId(),
                request.get("alcoholId").asLong(),
                nullableLong(request, "distilleryId"),
                nullableLong(request, "regionId")),
            target.getId());
    String classification = row.map(r -> r.classification()).orElse("NOT_IN_GROUP");
    List<String> reasons =
        row.map(r -> r.reasons().stream().map(MfdsBulkMatchingReasonItem::code).toList())
            .orElse(List.of());
    RESULTS.put(id, Stream.concat(Stream.of(classification), reasons.stream()).toList());

    // then
    List<String> expected = texts(scenario.get("reasons"));
    assertThat(classification).as(id).isEqualTo(scenario.get("expect").asText());
    switch (scenario.has("mode") ? scenario.get("mode").asText() : "EXACT") {
      case "EXACT" -> assertThat(reasons).as(id).containsExactlyInAnyOrderElementsOf(expected);
      case "CONTAINS" -> assertThat(reasons).as(id).containsAll(expected);
      case "ONLY" -> assertThat(reasons).as(id).isNotEmpty().containsOnlyElementsOf(expected);
      case "ANY" -> {}
      default -> throw new IllegalArgumentException(id);
    }
  }

  @Test
  @DisplayName("독립 제품 사례는 49종이고 연결 상태와 QA 제안을 빼도 30종 이상이다")
  void 독립_사례_수를_유지한다() {
    Set<String> proposals = Set.copyOf(texts(DATA.get("qaProposals")));
    List<JsonNode> independent =
        scenarios().filter(c -> c.get("id").asText().startsWith("IP")).toList();
    List<JsonNode> strict =
        independent.stream().filter(c -> !"LINKAGE_STATE".equals(category(c))).toList();

    assertThat(independent).hasSize(DATA.get("independentCount").asInt()).hasSize(49);
    assertThat(strict).hasSizeGreaterThanOrEqualTo(30);
    assertThat(strict.stream().filter(c -> !proposals.contains(c.get("id").asText())))
        .hasSizeGreaterThanOrEqualTo(30);
    assertThat(scenarios().map(c -> c.get("id").asText()).distinct())
        .hasSize((int) scenarios().count());
  }

  /** 사례별 실제 분류와 사유를 남겨 기존 기대값과 대조할 수 있게 한다. */
  @AfterAll
  static void writeResults() throws IOException {
    Path report = Path.of("build", "reports", "mfds-bulk-product-cases.json");
    Files.createDirectories(report.getParent());
    Files.writeString(report, MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(RESULTS));
  }

  private static Stream<JsonNode> scenarios() {
    return StreamSupport.stream(DATA.get("cases").spliterator(), false);
  }

  private static MfdsBulkTestFixture baselineFixture() {
    MfdsBulkTestFixture fixture = new MfdsBulkTestFixture();
    for (JsonNode a : baseline("alcohols")) {
      fixture.alcohols.addAlcohol(
          alcohol(
              a.get(0).asLong(),
              a.get(1).asText(),
              a.get(2).asText(),
              a.get(3).asLong(),
              a.get(4).asLong()));
    }
    baseline("distilleries")
        .forEach(
            id ->
                fixture.alcohols.addDistillery(
                    new DistilleryMatchTargetItem(id.asLong(), "증류소", "Distillery")));
    baseline("regions")
        .forEach(
            id ->
                fixture.alcohols.addRegion(new RegionMatchTargetItem(id.asLong(), "지역", "Region")));
    return fixture;
  }

  private static byte[] targetKey(JsonNode scenario) {
    if (!scenario.has("targetKey")) {
      return key(1);
    }
    return scenario.get("targetKey").isNull() ? null : key(2);
  }

  private static void overwrite(MfdsDeclaration declaration, JsonNode values) {
    if (values != null) {
      values
          .fields()
          .forEachRemaining(
              e -> MfdsTestData.set(declaration, e.getKey(), convert(e.getKey(), e.getValue())));
    }
  }

  private static void recordHistory(MfdsBulkTestFixture fixture, Long targetId, JsonNode history) {
    if (history == null) {
      return;
    }
    int order = 0;
    for (JsonNode action : history) {
      LocalDateTime at = LocalDateTime.of(2026, 9, 24, 0, 0).plusSeconds(++order);
      fixture.selections.save(
          "REVOKE".equals(action.asText())
              ? MfdsMatchingSelection.adminRevoke(
                  targetId, "ALCOHOL", 1L, "ADMIN_RELEASE", ADMIN_ID, at)
              : MfdsMatchingSelection.adminSelect(
                  targetId, "ALCOHOL", 1L, "ADMIN_SELECT", ADMIN_ID, at));
    }
  }

  private static Object convert(String field, JsonNode node) {
    if (node.isNull()) {
      return null;
    }
    return switch (field) {
      case "ageYears", "vintageYear" -> (short) node.asInt();
      case "abvPercent" -> new BigDecimal(node.asText());
      case "processedDate" -> LocalDate.parse(node.asText());
      case "normalizationStatus" -> MfdsNormalizationStatus.valueOf(node.asText());
      case "normalizationReasons" -> texts(node);
      case "importerId", "selectedAlcoholId", "selectedDistilleryId", "selectedRegionId" ->
          node.asLong();
      case "unitVolumeMl", "volumeMl" -> node.asInt();
      default -> node.asText();
    };
  }

  private static String category(JsonNode scenario) {
    return scenario.has("category") ? scenario.get("category").asText() : "PRODUCT_ATTRIBUTE";
  }

  private static JsonNode baseline(String field) {
    return DATA.get("baseline").get(field);
  }

  private static Long nullableLong(JsonNode node, String field) {
    return node.hasNonNull(field) ? node.get(field).asLong() : null;
  }

  private static List<String> texts(JsonNode node) {
    return node == null
        ? List.of()
        : StreamSupport.stream(node.spliterator(), false).map(JsonNode::asText).toList();
  }

  private static JsonNode load(String resource) {
    try (InputStream input = MfdsBulkProductCaseTest.class.getResourceAsStream(resource)) {
      return MAPPER.readTree(input);
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }
}
