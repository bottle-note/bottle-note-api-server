package app.bottlenote.curation.service;

import static org.assertj.core.api.Assertions.assertThat;

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
  @DisplayName("root array payload는 responseSpec.x-graphql 기준으로 BOTTLE_NOTE stats만 보강한다")
  void materialize_whenRootArrayPayload_hydratesBottleNoteStatsOnly() throws IOException {
    FakeCurationGraphqlExecutor executor =
        new FakeCurationGraphqlExecutor(
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
                    8)));
    CurationResponseMaterializer materializer = materializer(executor);
    List<Map<String, Object>> payload =
        List.of(
            item(
                "BOTTLE_NOTE",
                map("alcoholId", 1, "korName", "테스트", "selectedTags", List.of("셰리"))),
            item("MANUAL", map("alcoholId", null, "korName", "수동", "selectedTags", List.of("오크"))));

    Object result =
        materializer.materialize(1L, schema("recommended_whisky.json", "Response"), payload);

    JsonNode resultNode = OBJECT_MAPPER.valueToTree(result);
    assertThat(executor.executedQueries()).hasSize(1);
    assertThat(executor.executedQueries().get(0).query()).contains("alcohols(ids: $ids)");
    assertThat(executor.executedQueries().get(0).variables().get("ids")).isEqualTo(List.of(1L));
    assertThat(resultNode.get(0).path("stats").path("rating").asDouble()).isEqualTo(4.2);
    assertThat(resultNode.get(0).path("stats").has("alcoholId")).isFalse();
    assertThat(resultNode.get(1).path("stats").isNull()).isTrue();
    assertThat(resultNode.get(1).path("alcohol").path("korName").asText()).isEqualTo("수동");
  }

  @Test
  @DisplayName("object payload의 payloadPath=$.alcohols는 하위 alcohols 배열에만 stats를 보강한다")
  void materialize_whenNestedPayloadPath_hydratesOnlyAlcoholSubtree() throws IOException {
    FakeCurationGraphqlExecutor executor =
        new FakeCurationGraphqlExecutor(
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
        materializer.materialize(2L, schema("whisky_tasting_event.json", "Response"), payload);

    JsonNode resultNode = OBJECT_MAPPER.valueToTree(result);
    assertThat(executor.executedQueries().get(0).variables().get("ids")).isEqualTo(List.of(7L));
    assertThat(resultNode.path("eventDate").asText()).isEqualTo("2026-06-15");
    assertThat(resultNode.path("stats").isMissingNode()).isTrue();
    assertThat(resultNode.path("alcohols").get(0).path("stats").path("reviewCount").asInt())
        .isEqualTo(1);
  }

  private static CurationResponseMaterializer materializer(FakeCurationGraphqlExecutor executor) {
    return new CurationResponseMaterializer(
        OBJECT_MAPPER,
        new SpecGraphqlQueryBuilder(),
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

  private static final class FakeCurationGraphqlExecutor implements CurationGraphqlExecutor {

    private final List<Map<String, Object>> alcohols;
    private final List<SpecGraphqlQueryBuilder.Result> executedQueries = new ArrayList<>();

    FakeCurationGraphqlExecutor(List<Map<String, Object>> alcohols) {
      this.alcohols = alcohols;
    }

    @Override
    public Map<String, Object> execute(
        Long curationId, int index, SpecGraphqlQueryBuilder.Result query) {
      executedQueries.add(query);
      return map("data", map(query.entryField(), alcohols));
    }

    List<SpecGraphqlQueryBuilder.Result> executedQueries() {
      return executedQueries;
    }
  }
}
