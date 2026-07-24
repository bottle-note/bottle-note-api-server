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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@Tag("unit")
class CurationOpenApiSpecResourceTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
  private static final CurationPayloadValidator SPEC_VALIDATOR =
      new CurationPayloadValidator(OBJECT_MAPPER);
  private static final String SPEC_RESOURCE_PATTERN = "classpath*:openapi/curation/*.json";

  @Test
  @DisplayName("모든 큐레이션 OpenAPI 스펙은 필수 메타데이터와 요청 응답 스키마를 가진다")
  void curationOpenApiSpecs_whenLoaded_containCurationMetadata() throws IOException {
    List<Resource> resources = specResources();

    assertThat(resources).isNotEmpty();

    for (Resource resource : resources) {
      JsonNode root = OBJECT_MAPPER.readTree(resource.getInputStream());
      JsonNode curation = root.path("x-curation");
      JsonNode schemas = root.path("components").path("schemas");

      assertThat(root.path("openapi").asText()).isEqualTo("3.0.3");
      assertThat(root.path("paths").isObject()).isTrue();
      assertThat(curation.path("code").asText()).isNotBlank();
      assertThat(curation.path("hydratorKey").asText()).isNotBlank();
      assertThat(curation.path("container").asText()).isIn("array", "object");
      assertThat(schemas.isObject()).isTrue();
      assertThat(
              schemas.properties().stream().anyMatch(entry -> entry.getKey().endsWith("Request")))
          .isTrue();
      assertThat(
              schemas.properties().stream().anyMatch(entry -> entry.getKey().endsWith("Response")))
          .isTrue();
    }
  }

  @Test
  @DisplayName("큐레이션 OpenAPI 스펙의 x-depends-on은 필수 설명과 타입 정규화 가능한 값을 가진다")
  void curationOpenApiSpecs_whenFieldDependencyExists_followDependsOnContract() throws IOException {
    List<String> dependencyPaths = new ArrayList<>();

    for (Resource resource : specResources()) {
      JsonNode root = OBJECT_MAPPER.readTree(resource.getInputStream());
      JsonNode schemas = root.path("components").path("schemas");
      for (Map.Entry<String, JsonNode> schema : schemas.properties()) {
        String rootPath = resource.getFilename() + "#" + schema.getKey();
        Map<String, Object> schemaMap = OBJECT_MAPPER.convertValue(schema.getValue(), MAP_TYPE);

        assertThat(SPEC_VALIDATOR.validateSpec(rootPath, new MapBackedSchema(schemaMap))).isEmpty();
        collectDependencyPaths(rootPath, schema.getValue(), new ArrayDeque<>(), dependencyPaths);
      }
    }

    assertThat(dependencyPaths).isNotEmpty();
  }

  private static List<Resource> specResources() throws IOException {
    Resource[] resources =
        new PathMatchingResourcePatternResolver().getResources(SPEC_RESOURCE_PATTERN);
    return Arrays.stream(resources).sorted(Comparator.comparing(Resource::getFilename)).toList();
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
}
