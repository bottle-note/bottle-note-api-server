package app.bottlenote.curation.service;

import app.bottlenote.curation.dto.response.CurationFeedFieldResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurationFeedProjector {

  private static final String FEED_META = "x-feed";
  private static final String GRAPHQL_META = "x-graphql";
  private static final String PROPERTIES = "properties";
  private static final String ITEMS = "items";
  private static final String JSON_PATH_ROOT = "$";

  private final ObjectMapper objectMapper;

  public List<CurationFeedFieldResponse> project(Map<String, Object> responseSpec, Object payload) {
    JsonNode specNode = objectMapper.valueToTree(responseSpec);
    JsonNode payloadNode = objectMapper.valueToTree(payload);
    List<CurationFeedFieldResponse> fields = new ArrayList<>();
    collect(CurationFeedPaths.rootSchema(specNode), payloadNode, "", fields);
    return fields.stream()
        .sorted(
            Comparator.comparing(
                CurationFeedFieldResponse::order, Comparator.nullsLast(Integer::compareTo)))
        .toList();
  }

  // 조회 응답용: 보강까지 끝난 payload에서 x-feed 필드만 남긴다.
  public Object projectPayload(Map<String, Object> responseSpec, Object payload) {
    JsonNode specNode = objectMapper.valueToTree(responseSpec);
    JsonNode payloadNode = objectMapper.valueToTree(payload);
    JsonNode projected =
        projectNode(CurationFeedPaths.rootSchema(specNode), payloadNode, "", Set.of());
    return objectMapper.convertValue(projected, Object.class);
  }

  // 저장용 feed payload: x-feed 필드에 더해 피드에 필요한 x-graphql의 입력값까지 남긴다. GraphQL은 실행하지 않는다.
  public Object extractFeedPayload(Map<String, Object> responseSpec, Object payload) {
    JsonNode specNode = objectMapper.valueToTree(responseSpec);
    JsonNode payloadNode = objectMapper.valueToTree(payload);
    JsonNode rootSchema = CurationFeedPaths.rootSchema(specNode);
    JsonNode extracted = projectNode(rootSchema, payloadNode, "", feedInputPaths(rootSchema));
    // 남길 것이 없어도 null이 아니라 빈 컨테이너를 저장한다. null은 backfill 이전 레거시 행의 표시로만 쓴다.
    return objectMapper.convertValue(
        extracted != null ? extracted : emptyContainer(specNode, payloadNode), Object.class);
  }

  private JsonNode emptyContainer(JsonNode specNode, JsonNode payloadNode) {
    return CurationFeedPaths.isArrayContainer(specNode)
            || (payloadNode != null && payloadNode.isArray())
        ? objectMapper.createArrayNode()
        : objectMapper.createObjectNode();
  }

  // 피드 경로와 교차하는 x-graphql 엔트리의 입력값 경로. 조회 시 보강에 필요하지만 x-feed는 아닌 숨은 값이다.
  private Set<String> feedInputPaths(JsonNode rootSchema) {
    Set<String> feedPaths = CurationFeedPaths.collect(rootSchema);
    Set<String> inputPaths = new LinkedHashSet<>();
    collectFeedInputPaths(rootSchema, "", feedPaths, inputPaths);
    return inputPaths;
  }

  private void collectFeedInputPaths(
      JsonNode schema, String path, Set<String> feedPaths, Set<String> inputPaths) {
    if (schema == null || !schema.isObject()) {
      return;
    }
    JsonNode meta = schema.get(GRAPHQL_META);
    if (meta != null && meta.isObject() && meta.has("query")) {
      if (CurationFeedPaths.intersectsFeed(feedPaths, path)) {
        addFeedInputPath(meta, inputPaths);
      }
      return;
    }
    JsonNode properties = schema.get(PROPERTIES);
    if (properties != null && properties.isObject()) {
      properties
          .properties()
          .forEach(
              entry ->
                  collectFeedInputPaths(
                      entry.getValue(),
                      CurationFeedPaths.join(path, entry.getKey()),
                      feedPaths,
                      inputPaths));
    }
    JsonNode items = schema.get(ITEMS);
    if (items != null) {
      collectFeedInputPaths(items, path, feedPaths, inputPaths);
    }
  }

  // argFrom은 payloadPath로 내려간 노드(배열이면 그 원소) 기준 상대 경로이므로 둘을 이어 스키마 경로로 만든다.
  private void addFeedInputPath(JsonNode meta, Set<String> inputPaths) {
    String inputPath =
        CurationFeedPaths.join(
            CurationFeedPaths.normalize(meta.path("payloadPath").asText(JSON_PATH_ROOT)),
            CurationFeedPaths.normalize(meta.path("argFrom").asText(JSON_PATH_ROOT)));
    if (!inputPath.isBlank()) {
      inputPaths.add(inputPath);
    }
  }

  private void collect(
      JsonNode schema, JsonNode payload, String path, List<CurationFeedFieldResponse> fields) {
    if (schema == null || !schema.isObject()) {
      return;
    }
    JsonNode meta = schema.get(FEED_META);
    if (CurationFeedPaths.isEnabled(meta)) {
      fields.add(toField(path, meta, valueAt(payload, path)));
      return;
    }

    JsonNode properties = schema.get(PROPERTIES);
    if (properties == null || !properties.isObject()) {
      return;
    }
    properties
        .properties()
        .forEach(
            entry ->
                collect(
                    entry.getValue(),
                    payload,
                    CurationFeedPaths.join(path, entry.getKey()),
                    fields));
  }

  // keepPaths는 x-feed가 아니어도 남겨야 하는 스키마 경로다. 배열은 items 스키마로 재귀하므로 경로가 원소마다 어긋나지 않는다.
  private JsonNode projectNode(
      JsonNode schema, JsonNode payload, String path, Set<String> keepPaths) {
    if (schema == null || !schema.isObject() || payload == null || payload.isMissingNode()) {
      return null;
    }
    if (CurationFeedPaths.isEnabled(schema.get(FEED_META)) || keepPaths.contains(path)) {
      return payload;
    }
    if (payload.isArray()) {
      return projectArray(schema, payload, path, keepPaths);
    }
    if (!payload.isObject()) {
      return null;
    }

    JsonNode properties = schema.get(PROPERTIES);
    if (properties == null || !properties.isObject()) {
      return null;
    }
    ObjectNode projected = objectMapper.createObjectNode();
    properties
        .properties()
        .forEach(
            entry -> {
              JsonNode child =
                  projectNode(
                      entry.getValue(),
                      payload.get(entry.getKey()),
                      CurationFeedPaths.join(path, entry.getKey()),
                      keepPaths);
              if (child != null) {
                projected.set(entry.getKey(), child);
              }
            });
    return projected.isEmpty() ? null : projected;
  }

  private JsonNode projectArray(
      JsonNode schema, JsonNode payload, String path, Set<String> keepPaths) {
    JsonNode itemSchema = schema.has(ITEMS) ? schema.get(ITEMS) : schema;
    ArrayNode projected = objectMapper.createArrayNode();
    payload.forEach(
        item -> {
          JsonNode child = projectNode(itemSchema, item, path, keepPaths);
          if (child != null) {
            projected.add(child);
          }
        });
    return projected.isEmpty() ? null : projected;
  }

  private CurationFeedFieldResponse toField(String path, JsonNode meta, JsonNode value) {
    return new CurationFeedFieldResponse(
        path,
        meta.path("role").asText(null),
        meta.has("order") ? meta.get("order").asInt() : null,
        meta.path("description").asText(null),
        objectMapper.convertValue(value, Object.class));
  }

  private JsonNode valueAt(JsonNode payload, String path) {
    if (payload == null || path == null || path.isBlank() || JSON_PATH_ROOT.equals(path)) {
      return payload;
    }
    if (payload.isArray()) {
      return valuesFromArray(payload, path);
    }
    return GraphQLCurationQueryBuilder.navigate(payload, path);
  }

  private JsonNode valuesFromArray(JsonNode payload, String path) {
    var array = objectMapper.createArrayNode();
    payload.forEach(
        item -> {
          JsonNode value = GraphQLCurationQueryBuilder.navigate(item, path);
          array.add(value != null ? value : objectMapper.nullNode());
        });
    return array;
  }
}
