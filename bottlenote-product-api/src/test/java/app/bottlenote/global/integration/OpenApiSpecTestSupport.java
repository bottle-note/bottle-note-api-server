package app.bottlenote.global.integration;

import static java.nio.charset.StandardCharsets.UTF_8;

import app.bottlenote.IntegrationTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;

/** 생성된 OpenAPI 스펙을 읽어 검사하는 테스트들의 공통 기반. */
abstract class OpenApiSpecTestSupport extends IntegrationTestSupport {

  // 스펙 경로의 SSOT는 springdoc.api-docs.path 프로퍼티다
  @Value("${springdoc.api-docs.path}")
  protected String specPath;

  protected JsonNode fetchSpec() {
    var result = mockMvcTester.get().uri(specPath).exchange();
    try {
      return mapper.readTree(result.getResponse().getContentAsString(UTF_8));
    } catch (IOException e) {
      throw new UncheckedIOException("OpenAPI 스펙을 읽지 못했습니다", e);
    }
  }

  /** 스펙의 모든 엔드포인트를 평탄하게 펼친다. */
  protected List<SpecOperation> operationsOf(JsonNode spec) {
    return spec.at("/paths").properties().stream()
        .flatMap(
            path ->
                path.getValue().properties().stream()
                    .map(
                        method ->
                            new SpecOperation(path.getKey(), method.getKey(), method.getValue())))
        .toList();
  }

  /** 위반 목록을 실패 메시지에 담을 형태로 잇는다. */
  protected String joined(List<String> violations) {
    return String.join(System.lineSeparator(), violations);
  }

  /** 노드의 직접 자식 이름. */
  protected Stream<String> childNamesOf(JsonNode node) {
    return node.properties().stream().map(Map.Entry::getKey);
  }

  /** 스키마 노드가 선언한 property 이름. */
  protected List<String> propertyNamesOf(JsonNode node) {
    return childNamesOf(node.path("properties")).toList();
  }

  /** 스펙에 실린 하나의 엔드포인트. */
  protected record SpecOperation(String path, String method, JsonNode definition) {

    private static final String OBJECT_TYPE = "object";

    /** 200 응답 본문의 스키마. 공통 형식이면 그 형식, 아니면 DTO 참조. */
    JsonNode successSchema() {
      JsonNode content = definition.at("/responses/200/content");
      if (content.isMissingNode() || content.isEmpty()) {
        return MissingNode.getInstance();
      }
      return content.properties().iterator().next().getValue().path("schema");
    }

    /** HTTP 메서드와 경로를 합친 식별자. */
    String endpoint() {
      return "%s %s".formatted(method.toUpperCase(), path);
    }

    /** 문서에 선언된 보안 요구사항. 선언이 없으면 missing 노드. */
    JsonNode security() {
      return definition.path("security");
    }

    String tag() {
      JsonNode tags = definition.at("/tags");
      return tags.isArray() && !tags.isEmpty() ? tags.get(0).asText() : "";
    }

    String summary() {
      return definition.path("summary").asText("");
    }

    String operationId() {
      return definition.path("operationId").asText("");
    }

    /** 공통 형식의 data 자리에 담긴 스키마. */
    private JsonNode dataSchema() {
      return successSchema().path("properties").path("data");
    }

    boolean hasEmptyDataSchema() {
      JsonNode data = dataSchema();
      return OBJECT_TYPE.equals(data.path("type").asText())
          && !data.has("$ref")
          && !data.has("properties");
    }

    @Override
    public String toString() {
      return "%s %s (%s)".formatted(method.toUpperCase(), path, tag());
    }
  }
}
