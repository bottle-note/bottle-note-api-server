package app.bottlenote.curation.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurationSpecResourceReader {

  private static final String SPEC_RESOURCE_PATTERN = "classpath*:openapi/curation/*.json";
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

  private final ResourcePatternResolver resourcePatternResolver;
  private final ObjectMapper objectMapper;

  public List<CurationSpecResourceDocument> readAll() {
    return Arrays.stream(loadResources()).map(this::readSpec).toList();
  }

  private Resource[] loadResources() {
    try {
      Resource[] resources = resourcePatternResolver.getResources(SPEC_RESOURCE_PATTERN);
      Arrays.sort(resources, Comparator.comparing(Resource::getFilename));
      return resources;
    } catch (IOException e) {
      throw new IllegalStateException("큐레이션 스펙 리소스를 읽을 수 없습니다.", e);
    }
  }

  private CurationSpecResourceDocument readSpec(Resource resource) {
    try {
      JsonNode root = objectMapper.readTree(resource.getInputStream());
      JsonNode schemas = root.path("components").path("schemas");
      JsonNode requestSpec = findSchema(schemas, "Request");
      JsonNode responseSpec = findSchema(schemas, "Response");
      return new CurationSpecResourceDocument(
          root.path("x-curation").path("code").asText(),
          root.path("info").path("title").asText(),
          root.path("info").path("description").asText(null),
          objectMapper.convertValue(requestSpec, MAP_TYPE),
          objectMapper.convertValue(responseSpec, MAP_TYPE),
          root.path("x-curation").path("hydratorKey").asText(),
          parseMajorVersion(root.path("info").path("version").asText("1")));
    } catch (IOException e) {
      throw new IllegalStateException("큐레이션 스펙 리소스 파싱에 실패했습니다: " + resource.getFilename(), e);
    }
  }

  private JsonNode findSchema(JsonNode schemas, String suffix) {
    return schemas.properties().stream()
        .filter(entry -> entry.getKey().endsWith(suffix))
        .findFirst()
        .map(Map.Entry::getValue)
        .orElseThrow(() -> new IllegalStateException(suffix + " schema를 찾을 수 없습니다."));
  }

  private int parseMajorVersion(String version) {
    String major = version.split("\\.")[0];
    return Integer.parseInt(major);
  }

  public record CurationSpecResourceDocument(
      String code,
      String name,
      String description,
      Map<String, Object> requestSpec,
      Map<String, Object> responseSpec,
      String hydratorKey,
      Integer version) {}
}
