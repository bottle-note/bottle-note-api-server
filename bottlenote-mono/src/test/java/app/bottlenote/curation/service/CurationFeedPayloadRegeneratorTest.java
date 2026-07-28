package app.bottlenote.curation.service;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.curation.domain.CurationExtension;
import app.bottlenote.curation.domain.CurationSpec;
import app.bottlenote.curation.fixture.InMemoryCurationExtensionRepository;
import app.bottlenote.curation.fixture.InMemoryCurationSpecRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("CurationFeedPayloadRegenerator 단위 테스트")
class CurationFeedPayloadRegeneratorTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private InMemoryCurationSpecRepository specRepository;
  private InMemoryCurationExtensionRepository extensionRepository;
  private CurationFeedPayloadRegenerator regenerator;

  @BeforeEach
  void setUp() {
    specRepository = new InMemoryCurationSpecRepository();
    extensionRepository = new InMemoryCurationExtensionRepository();
    regenerator =
        new CurationFeedPayloadRegenerator(
            specRepository, extensionRepository, new CurationFeedProjector(OBJECT_MAPPER));
  }

  @Test
  @DisplayName("feed_payload가 NULL인 레거시 행도 재생성 대상이라 backfill을 겸한다")
  void regenerate_whenFeedPayloadIsNull_fillsIt() {
    CurationSpec spec = saveSpec(feedOnTitleSpec());
    saveExtension(1L, spec.getId(), payload("제목", "내부메모"), null);

    int regenerated = regenerator.regenerate(List.of(spec.getId()));

    assertThat(regenerated).isEqualTo(1);
    JsonNode feedPayload = feedPayloadOf(1L);
    assertThat(feedPayload.path("title").asText()).isEqualTo("제목");
    assertThat(feedPayload.has("internalNote")).isFalse();
  }

  @Test
  @DisplayName("스펙의 x-feed가 바뀌면 기존 feed_payload가 새 기준으로 갱신된다")
  void regenerate_whenSpecChanged_refreshesExistingFeedPayload() {
    CurationSpec spec = saveSpec(feedOnTitleSpec());
    saveExtension(1L, spec.getId(), payload("제목", "내부메모"), Map.of("title", "옛날값"));
    spec.update(
        spec.getName(),
        spec.getDescription(),
        spec.getRequestSpec(),
        feedOnBothSpec(),
        spec.getHydratorKey(),
        spec.getVersion(),
        true);

    regenerator.regenerate(List.of(spec.getId()));

    JsonNode feedPayload = feedPayloadOf(1L);
    assertThat(feedPayload.path("title").asText()).isEqualTo("제목");
    assertThat(feedPayload.path("internalNote").asText()).isEqualTo("내부메모");
  }

  @Test
  @DisplayName("대상이 아닌 스펙의 큐레이션은 건드리지 않는다")
  void regenerate_whenOtherSpec_leavesUntouched() {
    CurationSpec target = saveSpec(feedOnTitleSpec());
    CurationSpec other = saveSpec(feedOnTitleSpec());
    saveExtension(1L, target.getId(), payload("제목", "내부메모"), null);
    saveExtension(2L, other.getId(), payload("다른제목", "내부메모"), null);

    regenerator.regenerate(List.of(target.getId()));

    assertThat(extensionRepository.findByCurationId(1L).orElseThrow().getFeedPayload()).isNotNull();
    assertThat(extensionRepository.findByCurationId(2L).orElseThrow().getFeedPayload()).isNull();
  }

  @Test
  @DisplayName("원본 payload는 재생성 후에도 그대로다")
  void regenerate_keepsSourcePayloadIntact() {
    CurationSpec spec = saveSpec(feedOnTitleSpec());
    saveExtension(1L, spec.getId(), payload("제목", "내부메모"), null);

    regenerator.regenerate(List.of(spec.getId()));

    JsonNode payload =
        OBJECT_MAPPER.valueToTree(
            extensionRepository.findByCurationId(1L).orElseThrow().getPayload());
    assertThat(payload.path("internalNote").asText()).isEqualTo("내부메모");
  }

  @Test
  @DisplayName("빈 목록이나 null을 받으면 아무것도 하지 않는다")
  void regenerate_whenNoSpecIds_doesNothing() {
    assertThat(regenerator.regenerate(List.of())).isZero();
    assertThat(regenerator.regenerate(null)).isZero();
  }

  @Test
  @DisplayName("존재하지 않는 specId는 조용히 건너뛴다")
  void regenerate_whenSpecMissing_skips() {
    assertThat(regenerator.regenerate(List.of(9999L))).isZero();
  }

  private JsonNode feedPayloadOf(Long curationId) {
    return OBJECT_MAPPER.valueToTree(
        extensionRepository.findByCurationId(curationId).orElseThrow().getFeedPayload());
  }

  private CurationSpec saveSpec(Map<String, Object> responseSpec) {
    return specRepository.save(
        CurationSpec.builder()
            .code("SPEC_" + System.nanoTime())
            .name("테스트 스펙")
            .description("설명")
            .requestSpec(Map.of())
            .responseSpec(responseSpec)
            .hydratorKey("test")
            .version(1)
            .isActive(true)
            .build());
  }

  private void saveExtension(Long curationId, Long specId, Object payload, Object feedPayload) {
    extensionRepository.save(
        CurationExtension.builder()
            .curationId(curationId)
            .specId(specId)
            .payload(payload)
            .feedPayload(feedPayload)
            .build());
  }

  private static Map<String, Object> payload(String title, String internalNote) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("title", title);
    payload.put("internalNote", internalNote);
    return payload;
  }

  private static Map<String, Object> feedOnTitleSpec() {
    return Map.of(
        "type",
        "object",
        "properties",
        Map.of(
            "title", Map.of("type", "string", "x-feed", Map.of("enabled", true)),
            "internalNote", Map.of("type", "string")));
  }

  private static Map<String, Object> feedOnBothSpec() {
    return Map.of(
        "type",
        "object",
        "properties",
        Map.of(
            "title", Map.of("type", "string", "x-feed", Map.of("enabled", true)),
            "internalNote", Map.of("type", "string", "x-feed", Map.of("enabled", true))));
  }
}
