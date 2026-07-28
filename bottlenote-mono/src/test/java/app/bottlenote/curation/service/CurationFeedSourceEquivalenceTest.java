package app.bottlenote.curation.service;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.curation.domain.CurationExtension;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

// 읽기 경로 전환의 안전망. feed_payload를 소스로 써도 피드 응답이 원본 경로와 같아야 한다.
@Tag("unit")
@DisplayName("피드 소스 전환 동등성 단위 테스트")
class CurationFeedSourceEquivalenceTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

  private final CurationFeedProjector projector = new CurationFeedProjector(OBJECT_MAPPER);

  @Test
  @DisplayName("RECOMMENDED_WHISKY는 feed_payload로 투영해도 원본 투영과 같다")
  void projectPayload_whenRecommendedWhisky_isEquivalent() throws IOException {
    assertEquivalent(
        "recommended_whisky.json",
        List.of(
            map(
                "source", "BOTTLE_NOTE",
                "alcohol", map("alcoholId", 1, "korName", "테스트", "selectedTags", List.of("셰리")),
                "comment", "추천 코멘트")));
  }

  @Test
  @DisplayName("WHISKY_PAIRING은 feed_payload로 투영해도 원본 투영과 같다")
  void projectPayload_whenWhiskyPairing_isEquivalent() throws IOException {
    assertEquivalent(
        "whisky_pairing.json",
        List.of(
            map(
                "source",
                "BOTTLE_NOTE",
                "alcohol",
                map("alcoholId", 7, "korName", "페어링 위스키"),
                "comment",
                "페어링 코멘트",
                "pairings",
                List.of(map("itemName", "티라미수", "pairingNote", "단맛과 어울림")))));
  }

  @Test
  @DisplayName("WHISKY_TASTING_EVENT는 feed_payload로 투영해도 원본 투영과 같다")
  void projectPayload_whenTastingEvent_isEquivalent() throws IOException {
    assertEquivalent(
        "whisky_tasting_event.json",
        map(
            "eventDate",
            "2026-06-21",
            "eventTime",
            "19:00",
            "barAddress",
            "서울시 강남구 테스트로 1",
            "isRecruiting",
            true,
            "entryFee",
            50000,
            "capacity",
            12,
            "guideText",
            "시음회 안내",
            "alcohols",
            List.of(map("source", "BOTTLE_NOTE", "alcohol", map("alcoholId", 1)))));
  }

  @Test
  @DisplayName("PROGRAM은 feed_payload로 투영해도 원본 투영과 같다")
  void projectPayload_whenProgram_isEquivalent() throws IOException {
    assertEquivalent(
        "program.json",
        map(
            "eventStartDate", "2026-07-24",
            "placeName", "코엑스",
            "address", "서울 강남구 영동대로 513",
            "programTags", List.of("위스키"),
            "programs",
                List.of(
                    map(
                        "name", "오프닝",
                        "type", "TALK",
                        "programDate", "2026-07-24",
                        "startTime", "13:00",
                        "venue", "A홀"))));
  }

  @Test
  @DisplayName("feed_payload가 NULL이면 원본 payload를 소스로 쓴다")
  void feedSource_whenFeedPayloadIsNull_fallsBackToPayload() {
    Object payload = map("title", "제목");
    CurationExtension extension =
        CurationExtension.builder().curationId(1L).specId(1L).payload(payload).build();

    assertThat(extension.feedSource()).isSameAs(payload);
  }

  @Test
  @DisplayName("feed_payload가 비어 있어도 NULL이 아니면 그것을 소스로 쓴다")
  void feedSource_whenFeedPayloadIsEmpty_usesFeedPayload() {
    Object emptyFeedPayload = List.of();
    CurationExtension extension =
        CurationExtension.builder()
            .curationId(1L)
            .specId(1L)
            .payload(map("title", "제목"))
            .feedPayload(emptyFeedPayload)
            .build();

    assertThat(extension.feedSource()).isSameAs(emptyFeedPayload);
  }

  @Test
  @DisplayName("숨은 입력값은 feed_payload에 있어도 투영 후 응답에 남지 않는다")
  void projectPayload_whenHiddenInputExists_isNotExposed() {
    Map<String, Object> responseSpec =
        map(
            "type",
            "object",
            "properties",
            map(
                "title", map("type", "string", "x-feed", map("enabled", true)),
                "alcoholId", map("type", "integer"),
                "stats",
                    map(
                        "type", "object",
                        "x-feed", map("enabled", true),
                        "x-graphql",
                            map("query", "alcohols", "argFrom", "$.alcoholId", "writeTo", "stats"),
                        "properties", map("rating", map("type", "number", "x-graphql", true)))));
    Object payload = map("title", "제목", "alcoholId", 42);

    JsonNode feedPayload =
        OBJECT_MAPPER.valueToTree(projector.extractFeedPayload(responseSpec, payload));
    JsonNode projected =
        OBJECT_MAPPER.valueToTree(
            projector.projectPayload(
                responseSpec, OBJECT_MAPPER.convertValue(feedPayload, Object.class)));

    assertThat(feedPayload.path("alcoholId").asLong()).isEqualTo(42L);
    assertThat(projected.has("alcoholId")).isFalse();
    assertThat(projected.path("title").asText()).isEqualTo("제목");
  }

  @Test
  @DisplayName("현행 스펙에는 피드와 교차하는 x-graphql이 없다 — 전환 동등성의 전제다")
  void shippedSpecs_haveNoFeedIntersectingGraphQLEntry() throws IOException {
    for (String resourceName :
        List.of(
            "recommended_whisky.json",
            "whisky_pairing.json",
            "whisky_tasting_event.json",
            "program.json")) {
      JsonNode rootSchema =
          CurationFeedPaths.rootSchema(OBJECT_MAPPER.valueToTree(schema(resourceName)));
      Set<String> feedPaths = CurationFeedPaths.collect(rootSchema);
      List<String> intersecting = new ArrayList<>();
      collectFeedIntersectingGraphQLPaths(rootSchema, "", feedPaths, intersecting);

      assertThat(intersecting)
          .withFailMessage(
              """
              %s 에 피드와 교차하는 x-graphql 엔트리가 생겼다: %s
              이 경우 원본 경로는 인자가 비어도 writeTo에 null/[]을 채워 배열 원소를 살리지만,
              feed_payload 경로는 추출 단계에서 그 원소를 버려 응답이 갈릴 수 있다.
              읽기 경로 전환의 동등성을 다시 검토해야 한다.""",
              resourceName, intersecting)
          .isEmpty();
    }
  }

  private void collectFeedIntersectingGraphQLPaths(
      JsonNode schema, String path, Set<String> feedPaths, List<String> found) {
    if (schema == null || !schema.isObject()) {
      return;
    }
    JsonNode meta = schema.get("x-graphql");
    if (meta != null && meta.isObject() && meta.has("query")) {
      if (CurationFeedPaths.intersectsFeed(feedPaths, path)) {
        found.add(path);
      }
      return;
    }
    JsonNode properties = schema.get("properties");
    if (properties != null && properties.isObject()) {
      properties
          .properties()
          .forEach(
              entry ->
                  collectFeedIntersectingGraphQLPaths(
                      entry.getValue(),
                      CurationFeedPaths.join(path, entry.getKey()),
                      feedPaths,
                      found));
    }
    JsonNode items = schema.get("items");
    if (items != null) {
      collectFeedIntersectingGraphQLPaths(items, path, feedPaths, found);
    }
  }

  // 실제 피드 파이프라인(소스 → materializeFeed → projectPayload)을 두 소스로 각각 돌려 비교한다.
  private void assertEquivalent(String resourceName, Object payload) throws IOException {
    Map<String, Object> responseSpec = schema(resourceName);
    CurationResponseMaterializer materializer = materializer();

    JsonNode fromSource = feedResponseOf(materializer, responseSpec, payload);
    Object feedPayload = projector.extractFeedPayload(responseSpec, payload);
    JsonNode fromFeedPayload = feedResponseOf(materializer, responseSpec, feedPayload);

    assertThat(fromFeedPayload).isEqualTo(fromSource);
  }

  private JsonNode feedResponseOf(
      CurationResponseMaterializer materializer, Map<String, Object> responseSpec, Object source) {
    Object materialized = materializer.materializeFeed(1L, "TEST", responseSpec, source);
    return OBJECT_MAPPER.valueToTree(projector.projectPayload(responseSpec, materialized));
  }

  // 현재 4개 스펙에는 피드와 교차하는 x-graphql 엔트리가 없다. 실행되면 그 전제가 깨진 것이므로 실패시킨다.
  private CurationResponseMaterializer materializer() {
    return new CurationResponseMaterializer(
        OBJECT_MAPPER,
        new GraphQLCurationQueryBuilder(),
        (curationId, index, query) -> {
          throw new AssertionError("피드 경로에서 GraphQL이 실행되면 안 된다: " + query.entryField());
        },
        new CurationPayloadValidator(OBJECT_MAPPER));
  }

  private static Map<String, Object> schema(String resourceName) throws IOException {
    JsonNode root =
        OBJECT_MAPPER.readTree(
            new ClassPathResource("openapi/curation/" + resourceName).getInputStream());
    JsonNode schema =
        root.path("components").path("schemas").properties().stream()
            .filter(entry -> entry.getKey().endsWith("Response"))
            .findFirst()
            .map(Map.Entry::getValue)
            .orElseThrow();
    return OBJECT_MAPPER.convertValue(schema, MAP_TYPE);
  }

  private static Map<String, Object> map(Object... values) {
    Map<String, Object> map = new LinkedHashMap<>();
    for (int i = 0; i < values.length; i += 2) {
      map.put((String) values[i], values[i + 1]);
    }
    return map;
  }
}
