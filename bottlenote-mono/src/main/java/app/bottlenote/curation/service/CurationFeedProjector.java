package app.bottlenote.curation.service;

import app.bottlenote.curation.dto.response.CurationFeedFieldResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
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
    JsonNode rootSchema = rootSchema(specNode);
    List<CurationFeedFieldResponse> fields = new ArrayList<>();
    collect(rootSchema, payloadNode, "", fields);
    return fields.stream()
        .sorted(
            Comparator.comparing(
                CurationFeedFieldResponse::order, Comparator.nullsLast(Integer::compareTo)))
        .toList();
  }

  public Object projectPayload(Map<String, Object> responseSpec, Object payload) {
    JsonNode specNode = objectMapper.valueToTree(responseSpec);
    JsonNode payloadNode = objectMapper.valueToTree(payload);
    JsonNode projected = projectNode(rootSchema(specNode), payloadNode);
    return objectMapper.convertValue(projected, Object.class);
  }

  // 저장용 feed payload: x-feed 필드 투영 결과에 피드 교차 x-graphql의 숨은 입력값(argFrom)을 병합한다.
  public Object extractFeedPayload(Map<String, Object> responseSpec, Object payload) {
    JsonNode specNode = objectMapper.valueToTree(responseSpec);
    JsonNode payloadNode = objectMapper.valueToTree(payload);
    JsonNode rootSchema = rootSchema(specNode);
    Set<String> feedPaths = new HashSet<>();
    collectFeedPaths(rootSchema, "", feedPaths);
    List<String> inputPaths = new ArrayList<>();
    collectGraphQLInputPaths(rootSchema, "", feedPaths, inputPaths);
    if (payloadNode != null && payloadNode.isArray()) {
      ArrayNode extracted = objectMapper.createArrayNode();
      payloadNode.forEach(
          item -> {
            JsonNode child = extractObject(rootSchema, item, inputPaths);
            if (child != null) {
              extracted.add(child);
            }
          });
      return objectMapper.convertValue(extracted.isEmpty() ? null : extracted, Object.class);
    }
    return objectMapper.convertValue(
        extractObject(rootSchema, payloadNode, inputPaths), Object.class);
  }

  private JsonNode extractObject(JsonNode schema, JsonNode payload, List<String> inputPaths) {
    JsonNode projected = projectNode(schema, payload);
    if (projected == null || !projected.isObject() || payload == null || !payload.isObject()) {
      return projected;
    }
    for (String inputPath : inputPaths) {
      JsonNode value = GraphQLCurationQueryBuilder.navigate(payload, inputPath);
      if (value != null) {
        setAtPath((ObjectNode) projected, inputPath, value);
      }
    }
    return projected;
  }

  private void collectFeedPaths(JsonNode schema, String path, Set<String> paths) {
    if (schema == null || !schema.isObject()) {
      return;
    }
    if (isEnabled(schema.get(FEED_META))) {
      paths.add(path);
      return;
    }
    JsonNode properties = schema.get(PROPERTIES);
    if (properties != null && properties.isObject()) {
      properties
          .properties()
          .forEach(
              entry -> collectFeedPaths(entry.getValue(), append(path, entry.getKey()), paths));
    }
    JsonNode items = schema.get(ITEMS);
    if (items != null) {
      collectFeedPaths(items, path, paths);
    }
  }

  private void collectGraphQLInputPaths(
      JsonNode schema, String path, Set<String> feedPaths, List<String> inputPaths) {
    if (schema == null || !schema.isObject()) {
      return;
    }
    JsonNode meta = schema.get(GRAPHQL_META);
    if (meta != null && meta.isObject() && meta.has("query")) {
      String argFrom = normalizePath(meta.path("argFrom").asText(JSON_PATH_ROOT));
      boolean feedQuery = feedPaths.stream().anyMatch(feedPath -> intersects(feedPath, path));
      if (feedQuery && !argFrom.isBlank()) {
        inputPaths.add(argFrom);
      }
      return;
    }
    JsonNode properties = schema.get(PROPERTIES);
    if (properties != null && properties.isObject()) {
      properties
          .properties()
          .forEach(
              entry ->
                  collectGraphQLInputPaths(
                      entry.getValue(), append(path, entry.getKey()), feedPaths, inputPaths));
    }
    JsonNode items = schema.get(ITEMS);
    if (items != null) {
      collectGraphQLInputPaths(items, path, feedPaths, inputPaths);
    }
  }

  private boolean intersects(String feedPath, String targetPath) {
    if (feedPath.isBlank() || targetPath.isBlank()) {
      return true;
    }
    return targetPath.equals(feedPath)
        || targetPath.startsWith(feedPath + ".")
        || feedPath.startsWith(targetPath + ".");
  }

  private String normalizePath(String path) {
    if (path == null || JSON_PATH_ROOT.equals(path)) {
      return "";
    }
    return path.startsWith("$.") ? path.substring(2) : path;
  }

  private void setAtPath(ObjectNode target, String path, JsonNode value) {
    String[] segments = path.split("\\.");
    ObjectNode current = target;
    for (int i = 0; i < segments.length - 1; i++) {
      JsonNode next = current.get(segments[i]);
      if (next instanceof ObjectNode objectNode) {
        current = objectNode;
      } else {
        ObjectNode created = objectMapper.createObjectNode();
        current.set(segments[i], created);
        current = created;
      }
    }
    current.set(segments[segments.length - 1], value);
  }

  private JsonNode rootSchema(JsonNode specNode) {
    if ("array".equals(specNode.path("x-container").asText()) && specNode.has(ITEMS)) {
      return specNode.get(ITEMS);
    }
    return specNode;
  }

  private void collect(
      JsonNode schema, JsonNode payload, String path, List<CurationFeedFieldResponse> fields) {
    if (schema == null || !schema.isObject()) {
      return;
    }
    JsonNode meta = schema.get(FEED_META);
    if (isEnabled(meta)) {
      fields.add(toField(path, meta, valueAt(payload, path)));
      return;
    }

    JsonNode properties = schema.get(PROPERTIES);
    if (properties == null || !properties.isObject()) {
      return;
    }
    properties
        .properties()
        .forEach(entry -> collect(entry.getValue(), payload, append(path, entry.getKey()), fields));
  }

  private JsonNode projectNode(JsonNode schema, JsonNode payload) {
    if (schema == null || !schema.isObject() || payload == null || payload.isMissingNode()) {
      return null;
    }
    if (isEnabled(schema.get(FEED_META))) {
      return payload;
    }
    if (payload.isArray()) {
      return projectArray(schema, payload);
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
              JsonNode childPayload = payload.get(entry.getKey());
              JsonNode child = projectNode(entry.getValue(), childPayload);
              if (child != null) {
                projected.set(entry.getKey(), child);
              }
            });
    return projected.isEmpty() ? null : projected;
  }

  private JsonNode projectArray(JsonNode schema, JsonNode payload) {
    JsonNode itemSchema = schema.has(ITEMS) ? schema.get(ITEMS) : schema;
    ArrayNode projected = objectMapper.createArrayNode();
    payload.forEach(
        item -> {
          JsonNode child = projectNode(itemSchema, item);
          if (child != null) {
            projected.add(child);
          }
        });
    return projected.isEmpty() ? null : projected;
  }

  private boolean isEnabled(JsonNode meta) {
    return meta != null && meta.isObject() && meta.path("enabled").asBoolean(false);
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

  private String append(String path, String key) {
    return path == null || path.isBlank() ? key : path + "." + key;
  }
}
