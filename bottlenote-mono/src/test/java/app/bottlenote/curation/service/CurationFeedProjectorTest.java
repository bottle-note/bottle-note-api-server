package app.bottlenote.curation.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

@Tag("unit")
@DisplayName("CurationFeedProjector feed payload 추출 단위 테스트")
class CurationFeedProjectorTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

  private final CurationFeedProjector projector = new CurationFeedProjector(OBJECT_MAPPER);

  @Test
  @DisplayName("RECOMMENDED_WHISKY 추출 시 x-feed 필드(alcohol, comment)만 남고 source와 stats는 제외된다")
  void extractFeedPayload_whenRecommendedWhisky_keepsOnlyFeedFields() throws IOException {
    Map<String, Object> responseSpec = schema("recommended_whisky.json", "Response");
    Object payload =
        List.of(
            map(
                "source", "BOTTLE_NOTE",
                "alcohol", map("alcoholId", 1, "korName", "테스트", "selectedTags", List.of("셰리")),
                "comment", "추천 코멘트"),
            map(
                "source",
                "MANUAL",
                "alcohol",
                map("alcoholId", null, "korName", "수동", "selectedTags", List.of("오크")),
                "comment",
                null));

    JsonNode result =
        OBJECT_MAPPER.valueToTree(projector.extractFeedPayload(responseSpec, payload));

    assertThat(result).hasSize(2);
    JsonNode first = result.get(0);
    assertThat(first.size()).isEqualTo(2);
    assertThat(first.has("source")).isFalse();
    assertThat(first.has("stats")).isFalse();
    assertThat(first.path("alcohol").path("alcoholId").asLong()).isEqualTo(1L);
    assertThat(first.path("alcohol").path("korName").asText()).isEqualTo("테스트");
    assertThat(first.path("comment").asText()).isEqualTo("추천 코멘트");
    assertThat(result.get(1).path("alcohol").has("alcoholId")).isTrue();
    assertThat(result.get(1).path("comment").isNull()).isTrue();
  }

  @Test
  @DisplayName("WHISKY_PAIRING 추출 시 alcohol, comment, pairings만 남고 stats는 제외된다")
  void extractFeedPayload_whenWhiskyPairing_keepsPairingsAndDropsStats() throws IOException {
    Map<String, Object> responseSpec = schema("whisky_pairing.json", "Response");
    Object payload =
        List.of(
            map(
                "source",
                "BOTTLE_NOTE",
                "alcohol",
                map("alcoholId", 7, "korName", "페어링 위스키", "selectedTags", List.of("셰리")),
                "comment",
                "페어링 코멘트",
                "pairings",
                List.of(
                    map(
                        "itemName", "티라미수",
                        "itemImageUrl", "https://img.example.com/t.png",
                        "pairingNote", "단맛과 어울림"))));

    JsonNode result =
        OBJECT_MAPPER.valueToTree(projector.extractFeedPayload(responseSpec, payload));

    assertThat(result).hasSize(1);
    JsonNode first = result.get(0);
    assertThat(first.size()).isEqualTo(3);
    assertThat(first.has("source")).isFalse();
    assertThat(first.has("stats")).isFalse();
    assertThat(first.path("alcohol").path("alcoholId").asLong()).isEqualTo(7L);
    assertThat(first.path("pairings").get(0).path("itemName").asText()).isEqualTo("티라미수");
  }

  @Test
  @DisplayName("WHISKY_TASTING_EVENT 추출 시 x-feed가 없는 alcohols 라인업은 완전히 제외된다")
  void extractFeedPayload_whenTastingEvent_excludesAlcoholsLineup() throws IOException {
    Map<String, Object> responseSpec = schema("whisky_tasting_event.json", "Response");
    Object payload =
        map(
            "eventDate", "2026-06-21",
            "eventTime", "19:00",
            "barAddress", "서울시 강남구 테스트로 1",
            "detailAddress", "2층",
            "isRecruiting", true,
            "entryFee", 50000,
            "capacity", 12,
            "applicationLink", "https://example.com/apply",
            "guideText", "시음회 안내",
            "alcohols",
                List.of(
                    map(
                        "source",
                        "BOTTLE_NOTE",
                        "alcohol",
                        map("alcoholId", 1, "korName", "글렌드로낙 오리지널 12년"))));

    JsonNode result =
        OBJECT_MAPPER.valueToTree(projector.extractFeedPayload(responseSpec, payload));

    assertThat(result.has("alcohols")).isFalse();
    assertThat(result.path("eventDate").asText()).isEqualTo("2026-06-21");
    assertThat(result.path("isRecruiting").asBoolean()).isTrue();
    assertThat(result.path("capacity").asInt()).isEqualTo(12);
  }

  @Test
  @DisplayName("PROGRAM 추출 시 배열 안 중첩 객체는 x-feed 리프 필드(name, type, programDate, startTime)만 남는다")
  void extractFeedPayload_whenProgram_projectsNestedArrayLeafFields() throws IOException {
    Map<String, Object> responseSpec = schema("program.json", "Response");
    Object payload =
        map(
            "eventStartDate", "2026-07-24",
            "eventEndDate", "2026-07-26",
            "placeName", "코엑스",
            "address", "서울 강남구 영동대로 513",
            "entryFee", 30000,
            "officialUrl", "https://example.com",
            "programTags", List.of("위스키", "마스터클스"),
            "programs",
                List.of(
                    map(
                        "name", "오프닝",
                        "type", "TALK",
                        "programDate", "2026-07-24",
                        "startTime", "13:00",
                        "endTime", "14:00",
                        "venue", "A홀"),
                    map(
                        "name", "시음회",
                        "type", "TASTING",
                        "programDate", "2026-07-25",
                        "startTime", "15:00",
                        "whiskies", List.of(1, 2))));

    JsonNode result =
        OBJECT_MAPPER.valueToTree(projector.extractFeedPayload(responseSpec, payload));

    assertThat(result.has("address")).isFalse();
    assertThat(result.has("officialUrl")).isFalse();
    assertThat(result.path("placeName").asText()).isEqualTo("코엑스");
    assertThat(result.path("programTags")).hasSize(2);
    JsonNode programs = result.path("programs");
    assertThat(programs).hasSize(2);
    assertThat(programs.get(0).size()).isEqualTo(4);
    assertThat(programs.get(0).path("name").asText()).isEqualTo("오프닝");
    assertThat(programs.get(0).has("endTime")).isFalse();
    assertThat(programs.get(0).has("venue")).isFalse();
    assertThat(programs.get(1).has("whiskies")).isFalse();
  }

  @Test
  @DisplayName("x-feed 경로와 교차하는 x-graphql의 argFrom 값은 x-feed가 아니어도 숨은 입력값으로 보존한다")
  void extractFeedPayload_whenGraphQLInputIsHidden_preservesArgValue() {
    Map<String, Object> responseSpec =
        map(
            "type",
            "object",
            "properties",
            map(
                "title", map("type", "string", "x-feed", map("enabled", true, "role", "title")),
                "alcoholId", map("type", "integer"),
                "internalNote", map("type", "string"),
                "stats",
                    map(
                        "type", "object",
                        "nullable", true,
                        "x-feed", map("enabled", true, "role", "stats"),
                        "x-graphql",
                            map(
                                "query", "alcohols",
                                "argFrom", "$.alcoholId",
                                "argName", "ids",
                                "argType", "[ID!]!",
                                "writeTo", "stats",
                                "resultKey", "alcoholId"),
                        "properties", map("rating", map("type", "number", "x-graphql", true))),
                "extra",
                    map(
                        "type", "object",
                        "x-graphql", map("query", "alcohols", "argFrom", "$.internalNote"),
                        "properties", map("rating", map("type", "number", "x-graphql", true)))));
    Object payload = map("title", "제목", "alcoholId", 42, "internalNote", "노출금지");

    JsonNode result =
        OBJECT_MAPPER.valueToTree(projector.extractFeedPayload(responseSpec, payload));

    assertThat(result.path("title").asText()).isEqualTo("제목");
    assertThat(result.path("alcoholId").asLong()).isEqualTo(42L);
    assertThat(result.has("internalNote")).isFalse();
    assertThat(result.has("extra")).isFalse();
    assertThat(result.has("stats")).isFalse();
  }

  private static Map<String, Object> schema(String resourceName, String suffix) throws IOException {
    JsonNode root =
        OBJECT_MAPPER.readTree(
            new ClassPathResource("openapi/curation/" + resourceName).getInputStream());
    JsonNode schemas = root.path("components").path("schemas");
    JsonNode schema =
        schemas.properties().stream()
            .filter(entry -> entry.getKey().endsWith(suffix))
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
