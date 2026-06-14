package app.bottlenote.curation.service;

import app.bottlenote.curation.dto.response.CurationFeedFieldResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurationFeedProjector {

  private static final String FEED_META = "x-feed";
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
