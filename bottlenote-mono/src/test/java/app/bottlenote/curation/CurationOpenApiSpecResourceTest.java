package app.bottlenote.curation;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.curation.service.CurationPayloadValidator;
import app.bottlenote.curation.service.CurationPayloadValidator.MapBackedSchema;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

@Tag("unit")
class CurationOpenApiSpecResourceTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
  private static final CurationPayloadValidator SPEC_VALIDATOR =
      new CurationPayloadValidator(OBJECT_MAPPER);

  @Test
  @DisplayName("큐레이션 OpenAPI 스펙 3종은 리소스에 포함되고 GraphQL 보강 메타를 가진다")
  void curationOpenApiSpecs_whenLoaded_containCurationMetadata() throws IOException {
    List<SpecResource> specs = specResources();

    for (SpecResource spec : specs) {
      ClassPathResource resource = new ClassPathResource(spec.path());

      assertThat(resource.exists()).isTrue();

      JsonNode root = OBJECT_MAPPER.readTree(resource.getInputStream());

      assertThat(root.path("openapi").asText()).isEqualTo("3.0.3");
      assertThat(root.path("paths").isObject()).isTrue();
      assertThat(root.path("x-curation").path("code").asText()).isEqualTo(spec.code());
      assertThat(root.path("x-curation").path("hydratorKey").asText()).isEqualTo("alcohol");
      assertThat(root.path("x-curation").path("container").asText()).isEqualTo(spec.container());
      assertThat(root.path("components").path("schemas").isObject()).isTrue();
      assertThat(root.toString())
          .contains(
              "\"source\"",
              "\"BOTTLE_NOTE\"",
              "\"MANUAL\"",
              "\"alcoholId\"",
              "\"stats\"",
              "\"x-graphql\"",
              "\"totalRatingsCount\"",
              "\"reviewCount\"",
              "\"totalPickCount\"");
    }
  }

  @Test
  @DisplayName("큐레이션 OpenAPI 스펙의 x-depends-on은 필수 설명과 타입 정규화 가능한 값을 가진다")
  void curationOpenApiSpecs_whenFieldDependencyExists_followDependsOnContract() throws IOException {
    List<String> dependencyPaths = new ArrayList<>();

    for (SpecResource spec : specResources()) {
      JsonNode root = OBJECT_MAPPER.readTree(new ClassPathResource(spec.path()).getInputStream());
      JsonNode schemas = root.path("components").path("schemas");
      for (Map.Entry<String, JsonNode> schema : schemas.properties()) {
        String rootPath = spec.path() + "#" + schema.getKey();
        Map<String, Object> schemaMap = OBJECT_MAPPER.convertValue(schema.getValue(), MAP_TYPE);

        assertThat(SPEC_VALIDATOR.validateSpec(rootPath, new MapBackedSchema(schemaMap))).isEmpty();
        collectDependencyPaths(rootPath, schema.getValue(), new ArrayDeque<>(), dependencyPaths);
      }
    }

    assertThat(dependencyPaths)
        .containsExactlyInAnyOrder(
            "openapi/curation/recommended_whisky.json#RecommendedWhiskyItemResponse.stats",
            "openapi/curation/whisky_pairing.json#WhiskyPairingItemResponse.stats",
            "openapi/curation/whisky_tasting_event.json#WhiskyTastingEventRequest.applicationLink",
            "openapi/curation/whisky_tasting_event.json#WhiskyTastingEventResponse.applicationLink",
            "openapi/curation/whisky_tasting_event.json#WhiskyTastingEventResponse.alcohols[].stats");
  }

  private static List<SpecResource> specResources() {
    return List.of(
        new SpecResource("openapi/curation/recommended_whisky.json", "RECOMMENDED_WHISKY", "array"),
        new SpecResource("openapi/curation/whisky_pairing.json", "WHISKY_PAIRING", "array"),
        new SpecResource(
            "openapi/curation/whisky_tasting_event.json", "WHISKY_TASTING_EVENT", "object"));
  }

  private static void collectDependencyPaths(
      String rootPath, JsonNode schema, ArrayDeque<String> path, List<String> dependencyPaths) {
    if (schema.has("x-depends-on")) {
      dependencyPaths.add(rootPath + "." + String.join(".", path));
    }

    JsonNode properties = schema.path("properties");
    if (properties.isObject()) {
      for (Map.Entry<String, JsonNode> property : properties.properties()) {
        path.addLast(property.getKey());
        collectDependencyPaths(rootPath, property.getValue(), path, dependencyPaths);
        path.removeLast();
      }
    }

    JsonNode items = schema.path("items");
    if (items.isObject()) {
      String arrayField = path.removeLast();
      path.addLast(arrayField + "[]");
      collectDependencyPaths(rootPath, items, path, dependencyPaths);
      path.removeLast();
      path.addLast(arrayField);
    }
  }

  private record SpecResource(String path, String code, String container) {}
}
