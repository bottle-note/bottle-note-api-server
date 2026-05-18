package app.bottlenote.curation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.curation.exception.CurationException;
import app.bottlenote.curation.exception.CurationExceptionCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

@Tag("unit")
@DisplayName("CurationResponseMaterializer 단위 테스트")
class CurationResponseMaterializerTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

  @Test
  @DisplayName("source: BOTTLE_NOTE(내부 알코올 참조)는 저장된 메타 정보를 유지하고 stats만 GraphQL 결과로 보강한다")
  void materialize_whenRootArrayPayload_hydratesBottleNoteStatsOnly() throws IOException {
    FakeGraphQLCurationExecutor executor =
        new FakeGraphQLCurationExecutor(
            List.of(
                map(
                    "alcoholId",
                    "1",
                    "rating",
                    4.2,
                    "totalRatingsCount",
                    10,
                    "reviewCount",
                    3,
                    "totalPickCount",
                    8,
                    "korName",
                    "GraphQL 원본명",
                    "selectedTags",
                    List.of("GraphQL 태그"))));
    CurationResponseMaterializer materializer = materializer(executor);
    List<Map<String, Object>> payload =
        List.of(
            item(
                "BOTTLE_NOTE",
                map("alcoholId", 1, "korName", "테스트", "selectedTags", List.of("셰리"))),
            item("MANUAL", map("alcoholId", null, "korName", "수동", "selectedTags", List.of("오크"))));

    Object result =
        materializer.materialize(
            1L, "RECOMMENDED_WHISKY", schema("recommended_whisky.json", "Response"), payload);

    JsonNode resultNode = OBJECT_MAPPER.valueToTree(result);
    assertThat(executor.executedQueries()).hasSize(1);
    assertThat(executor.executedQueries().get(0).query()).contains("alcohols(ids: $ids)");
    assertThat(executor.executedQueries().get(0).variables().get("ids")).isEqualTo(List.of(1L));
    assertThat(resultNode.get(0).path("alcohol").path("korName").asText()).isEqualTo("테스트");
    assertThat(resultNode.get(0).path("alcohol").path("korName").asText())
        .isNotEqualTo("GraphQL 원본명");
    assertThat(resultNode.get(0).path("alcohol").path("selectedTags").get(0).asText())
        .isEqualTo("셰리");
    assertThat(resultNode.get(0).path("alcohol").path("selectedTags").get(0).asText())
        .isNotEqualTo("GraphQL 태그");
    assertThat(resultNode.get(0).path("stats").path("rating").asDouble()).isEqualTo(4.2);
    assertThat(resultNode.get(0).path("stats").path("totalRatingsCount").asInt()).isEqualTo(10);
    assertThat(resultNode.get(0).path("stats").has("alcoholId")).isFalse();
    assertThat(resultNode.get(1).path("stats").isNull()).isTrue();
    assertThat(resultNode.get(1).path("alcohol").path("korName").asText()).isEqualTo("수동");
  }

  @Test
  @DisplayName(
      "source: BOTTLE_NOTE(내부 알코올 참조)가 중복 alcoholId를 가질 경우 GraphQL 변수는 중복 제거하고 각 항목에 stats를 보강한다")
  void materialize_whenDuplicateBottleNoteAlcoholId_deduplicatesQueryVariablesAndHydratesEachItem()
      throws IOException {
    FakeGraphQLCurationExecutor executor =
        new FakeGraphQLCurationExecutor(
            List.of(
                map(
                    "alcoholId",
                    11,
                    "rating",
                    4.7,
                    "totalRatingsCount",
                    5,
                    "reviewCount",
                    2,
                    "totalPickCount",
                    9)));
    CurationResponseMaterializer materializer = materializer(executor);
    List<Map<String, Object>> payload =
        List.of(
            item(
                "BOTTLE_NOTE",
                map("alcoholId", 11, "korName", "첫 번째 스냅샷", "selectedTags", List.of("셰리"))),
            item(
                "BOTTLE_NOTE",
                map("alcoholId", 11, "korName", "두 번째 스냅샷", "selectedTags", List.of("피트"))),
            item(
                "MANUAL",
                map("alcoholId", null, "korName", "직접 입력", "selectedTags", List.of("오크"))));

    Object result =
        materializer.materialize(
            1L, "RECOMMENDED_WHISKY", schema("recommended_whisky.json", "Response"), payload);

    JsonNode resultNode = OBJECT_MAPPER.valueToTree(result);
    assertThat(executor.executedQueries()).hasSize(1);
    assertThat(executor.executedQueries().get(0).variables().get("ids")).isEqualTo(List.of(11L));
    assertThat(resultNode.get(0).path("alcohol").path("korName").asText()).isEqualTo("첫 번째 스냅샷");
    assertThat(resultNode.get(1).path("alcohol").path("korName").asText()).isEqualTo("두 번째 스냅샷");
    assertThat(resultNode.get(0).path("stats").path("rating").asDouble()).isEqualTo(4.7);
    assertThat(resultNode.get(1).path("stats").path("rating").asDouble()).isEqualTo(4.7);
    assertThat(resultNode.get(0).path("stats").path("totalPickCount").asInt()).isEqualTo(9);
    assertThat(resultNode.get(1).path("stats").path("totalPickCount").asInt()).isEqualTo(9);
    assertThat(resultNode.get(2).path("stats").isNull()).isTrue();
  }

  @Test
  @DisplayName("source: MANUAL(직접 입력)만 있을 경우 GraphQL을 실행하지 않고 저장된 payload 그대로 응답한다")
  void materialize_whenOnlyManualItems_doesNotExecuteGraphQLAndKeepsSnapshotPayload()
      throws IOException {
    FakeGraphQLCurationExecutor executor = new FakeGraphQLCurationExecutor(List.of());
    CurationResponseMaterializer materializer = materializer(executor);
    List<Map<String, Object>> payload =
        List.of(
            item(
                "MANUAL",
                map("alcoholId", null, "korName", "직접 입력 위스키", "selectedTags", List.of("오크"))));

    Object result =
        materializer.materialize(
            1L, "RECOMMENDED_WHISKY", schema("recommended_whisky.json", "Response"), payload);

    JsonNode resultNode = OBJECT_MAPPER.valueToTree(result);
    assertThat(executor.executedQueries()).isEmpty();
    assertThat(resultNode.get(0).path("source").asText()).isEqualTo("MANUAL");
    assertThat(resultNode.get(0).path("alcohol").path("korName").asText()).isEqualTo("직접 입력 위스키");
    assertThat(resultNode.get(0).path("alcohol").path("selectedTags").get(0).asText())
        .isEqualTo("오크");
    assertThat(resultNode.get(0).path("stats").isNull()).isTrue();
  }

  @Test
  @DisplayName("object payload의 payloadPath=$.alcohols는 하위 alcohols 배열에만 stats를 보강한다")
  void materialize_whenNestedPayloadPath_hydratesOnlyAlcoholSubtree() throws IOException {
    FakeGraphQLCurationExecutor executor =
        new FakeGraphQLCurationExecutor(
            List.of(
                map(
                    "alcoholId",
                    7,
                    "rating",
                    3.8,
                    "totalRatingsCount",
                    4,
                    "reviewCount",
                    1,
                    "totalPickCount",
                    2)));
    CurationResponseMaterializer materializer = materializer(executor);
    Map<String, Object> payload =
        map(
            "eventDate",
            "2026-06-15",
            "eventTime",
            "19:30",
            "barAddress",
            "서울 강남구",
            "detailAddress",
            "2층 도시남 바",
            "isRecruiting",
            true,
            "entryFee",
            0,
            "capacity",
            20,
            "applicationLink",
            "https://forms.example.com/tasting",
            "guideText",
            "안내",
            "alcohols",
            List.of(
                item(
                    "BOTTLE_NOTE",
                    map("alcoholId", 7, "korName", "테스트", "selectedTags", List.of("셰리")))));

    Object result =
        materializer.materialize(
            2L, "WHISKY_TASTING_EVENT", schema("whisky_tasting_event.json", "Response"), payload);

    JsonNode resultNode = OBJECT_MAPPER.valueToTree(result);
    assertThat(executor.executedQueries().get(0).variables().get("ids")).isEqualTo(List.of(7L));
    assertThat(resultNode.path("eventDate").asText()).isEqualTo("2026-06-15");
    assertThat(resultNode.path("stats").isMissingNode()).isTrue();
    assertThat(resultNode.path("alcohols").get(0).path("stats").path("reviewCount").asInt())
        .isEqualTo(1);
  }

  @Test
  @DisplayName("GraphQL 실행 errors가 있으면 부분 응답을 만들지 않고 실패한다")
  void materialize_whenGraphQLResultHasErrors_throwsExecutionFailed() throws IOException {
    CurationResponseMaterializer materializer = materializer(new ErrorGraphQLCurationExecutor());
    List<Map<String, Object>> payload =
        List.of(
            item(
                "BOTTLE_NOTE",
                map("alcoholId", 1, "korName", "테스트", "selectedTags", List.of("셰리"))));

    assertThatThrownBy(
            () ->
                materializer.materialize(
                    1L,
                    "RECOMMENDED_WHISKY",
                    schema("recommended_whisky.json", "Response"),
                    payload))
        .isInstanceOf(CurationException.class)
        .hasFieldOrPropertyWithValue(
            "exceptionCode", CurationExceptionCode.CURATION_GRAPHQL_EXECUTION_FAILED);
  }

  private static CurationResponseMaterializer materializer(GraphQLCurationExecutor executor) {
    return new CurationResponseMaterializer(
        OBJECT_MAPPER,
        new GraphQLCurationQueryBuilder(),
        executor,
        new CurationPayloadValidator(OBJECT_MAPPER));
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

  private static Map<String, Object> item(String source, Map<String, Object> alcohol) {
    return map("source", source, "alcohol", alcohol, "comment", null);
  }

  private static Map<String, Object> map(Object... values) {
    Map<String, Object> map = new LinkedHashMap<>();
    for (int i = 0; i < values.length; i += 2) {
      map.put((String) values[i], values[i + 1]);
    }
    return map;
  }

  private static final class FakeGraphQLCurationExecutor implements GraphQLCurationExecutor {

    private final List<Map<String, Object>> alcohols;
    private final List<GraphQLCurationQueryBuilder.Result> executedQueries = new ArrayList<>();

    FakeGraphQLCurationExecutor(List<Map<String, Object>> alcohols) {
      this.alcohols = alcohols;
    }

    @Override
    public Map<String, Object> execute(
        Long curationId, int index, GraphQLCurationQueryBuilder.Result query) {
      executedQueries.add(query);
      return map("data", map(query.entryField(), alcohols));
    }

    List<GraphQLCurationQueryBuilder.Result> executedQueries() {
      return executedQueries;
    }
  }

  private static final class ErrorGraphQLCurationExecutor implements GraphQLCurationExecutor {

    @Override
    public Map<String, Object> execute(
        Long curationId, int index, GraphQLCurationQueryBuilder.Result query) {
      return map("errors", List.of(map("message", "forced graphql error")));
    }
  }
}
