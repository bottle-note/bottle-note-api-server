package app.bottlenote.global.integration;

import static java.nio.charset.StandardCharsets.UTF_8;

import app.bottlenote.IntegrationTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import java.util.ArrayList;
import java.util.List;

/** 생성된 OpenAPI 스펙을 읽어 검사하는 테스트들의 공통 기반. */
abstract class OpenApiSpecTestSupport extends IntegrationTestSupport {

  protected static final String SPEC_PATH = "/openapi.product.json";
  protected static final String JSON_SCHEMA = "/content/application~1json/schema";

  protected JsonNode fetchSpec() throws Exception {
    var result = mockMvcTester.get().uri(SPEC_PATH).exchange();
    return mapper.readTree(result.getResponse().getContentAsString(UTF_8));
  }

  /** 스펙의 모든 엔드포인트를 평탄하게 펼친다. */
  protected List<SpecOperation> operationsOf(JsonNode spec) {
    List<SpecOperation> operations = new ArrayList<>();
    spec.at("/paths")
        .properties()
        .forEach(
            pathEntry ->
                pathEntry
                    .getValue()
                    .properties()
                    .forEach(
                        methodEntry ->
                            operations.add(
                                new SpecOperation(
                                    pathEntry.getKey(),
                                    methodEntry.getKey(),
                                    methodEntry.getValue()))));
    return operations;
  }

  /** 지정한 자식 노드의 필드 이름 목록. */
  protected List<String> fieldNamesOf(JsonNode node, String childName) {
    List<String> names = new ArrayList<>();
    node.path(childName).fieldNames().forEachRemaining(names::add);
    return names;
  }

  protected List<String> fieldNamesOf(JsonNode node) {
    List<String> names = new ArrayList<>();
    node.path("properties").fieldNames().forEachRemaining(names::add);
    return names;
  }

  /** 스펙에 실린 하나의 엔드포인트. */
  protected record SpecOperation(String path, String method, JsonNode definition) {

    /** 200 응답 본문의 스키마. 공통 형식이면 그 형식, 아니면 DTO 참조. */
    JsonNode successSchema() {
      JsonNode content = definition.at("/responses/200/content");
      if (content.isMissingNode() || content.isEmpty()) {
        return MissingNode.getInstance();
      }
      return content.properties().iterator().next().getValue().path("schema");
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

    JsonNode dataSchema() {
      return definition.at("/responses/200" + JSON_SCHEMA + "/properties/data");
    }

    boolean hasEmptyDataSchema() {
      JsonNode data = dataSchema();
      return "object".equals(data.path("type").asText())
          && !data.has("$ref")
          && !data.has("properties");
    }

    @Override
    public String toString() {
      return "%s %s (%s)".formatted(method.toUpperCase(), path, tag());
    }
  }
}
