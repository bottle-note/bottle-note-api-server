package app.bottlenote.curation.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class GraphQLCurationQueryBuilder {

  static final String WRITE_MODE_ARRAY = "array";
  static final String WRITE_MODE_SINGLE = "single";

  private static final String META_KEY = "x-graphql";
  private static final String JSON_PATH_ROOT = "$";
  private static final String JSON_PATH_PREFIX = "$.";
  private static final String DEFAULT_ARG_NAME = "id";
  private static final String DEFAULT_ARG_TYPE = "ID!";
  private static final String DEFAULT_RESULT_KEY = "alcoholId";

  public List<Result> build(JsonNode responseSpec, JsonNode payload) {
    List<Result> results = new ArrayList<>();
    walk(responseSpec, payload, results, "");
    return results;
  }

  static JsonNode navigate(JsonNode root, String path) {
    if (root == null || path == null || JSON_PATH_ROOT.equals(path)) {
      return root;
    }
    String trimmed = stripPathPrefix(path);
    if (trimmed.isEmpty()) {
      return root;
    }
    JsonNode current = root;
    for (String segment : trimmed.split("\\.")) {
      if (current == null) {
        return null;
      }
      current = current.get(segment);
    }
    return current;
  }

  private void walk(JsonNode node, JsonNode payload, List<Result> results, String path) {
    if (node == null || !node.isObject()) {
      return;
    }
    JsonNode meta = node.get(META_KEY);
    if (isEntryPointMeta(meta)) {
      results.add(buildOne(node, meta, payload, path));
      return;
    }
    JsonNode properties = node.get("properties");
    if (properties != null && properties.isObject()) {
      properties
          .properties()
          .forEach(entry -> walk(entry.getValue(), payload, results, append(path, entry.getKey())));
    }
    JsonNode items = node.get("items");
    if (items != null) {
      walk(items, payload, results, path);
    }
  }

  private boolean isEntryPointMeta(JsonNode meta) {
    return meta != null && meta.isObject() && meta.has("query");
  }

  private Result buildOne(JsonNode entry, JsonNode meta, JsonNode payload, String targetPath) {
    String queryName = meta.get("query").asText();
    String argName = meta.path("argName").asText(DEFAULT_ARG_NAME);
    String argType = meta.path("argType").asText(DEFAULT_ARG_TYPE);
    String argFrom = meta.path("argFrom").asText(JSON_PATH_ROOT);
    String writeTo = meta.has("writeTo") ? meta.get("writeTo").asText(null) : null;
    String resultKey = meta.path("resultKey").asText(DEFAULT_RESULT_KEY);
    String payloadPath = meta.path("payloadPath").asText(JSON_PATH_ROOT);

    JsonNode subPayload = navigate(payload, payloadPath);
    Map<String, Object> variables = new LinkedHashMap<>();
    variables.put(argName, extractArg(subPayload, argFrom));

    List<String> selectionFields =
        new ArrayList<>(collectSelection(resolveSelectionRoot(entry, writeTo)));
    if (!selectionFields.contains(resultKey)) {
      selectionFields.add(0, resultKey);
    }
    String query =
        String.format(
            "query Q($%s: %s) { %s(%s: $%s) { %s } }",
            argName, argType, queryName, argName, argName, String.join(" ", selectionFields));
    return new Result(
        query,
        variables,
        queryName,
        argFrom,
        writeTo,
        resolveWriteMode(entry, writeTo),
        resultKey,
        payloadPath,
        targetPath);
  }

  private String append(String path, String key) {
    return path == null || path.isBlank() ? key : path + "." + key;
  }

  private JsonNode resolveSelectionRoot(JsonNode entry, String writeTo) {
    if (writeTo != null) {
      JsonNode target = entry.path("properties").path(writeTo);
      if (target.isMissingNode()) {
        return entry;
      }
      return target.has("items") ? target.get("items") : target;
    }
    return "array".equals(entry.path("type").asText()) && entry.has("items")
        ? entry.get("items")
        : entry;
  }

  private String resolveWriteMode(JsonNode entry, String writeTo) {
    if (writeTo == null) {
      return WRITE_MODE_ARRAY;
    }
    String type = entry.path("properties").path(writeTo).path("type").asText();
    return WRITE_MODE_ARRAY.equals(type) ? WRITE_MODE_ARRAY : WRITE_MODE_SINGLE;
  }

  private List<String> collectSelection(JsonNode node) {
    List<String> selections = new ArrayList<>();
    if (node == null || !node.path("properties").isObject()) {
      return selections;
    }
    for (Map.Entry<String, JsonNode> entry : node.path("properties").properties()) {
      JsonNode meta = entry.getValue().get(META_KEY);
      if (meta == null || isEntryPointMeta(meta)) {
        continue;
      }
      selections.add(resolveFieldName(entry.getKey(), meta));
    }
    return selections;
  }

  private String resolveFieldName(String key, JsonNode meta) {
    if (meta.isTextual()) {
      return meta.asText();
    }
    if (meta.isObject() && meta.has("field")) {
      return meta.get("field").asText(key);
    }
    return key;
  }

  private Object extractArg(JsonNode payload, String argFrom) {
    if (payload == null) {
      return null;
    }
    if (payload.isArray()) {
      Set<Object> values = new LinkedHashSet<>();
      payload.forEach(element -> addFlat(values, readPath(element, argFrom)));
      values.remove(null);
      return new ArrayList<>(values);
    }
    Object value = readPath(payload, argFrom);
    if (value instanceof List<?> list) {
      Set<Object> values = new LinkedHashSet<>(list);
      values.remove(null);
      return new ArrayList<>(values);
    }
    return value;
  }

  private void addFlat(Set<Object> values, Object value) {
    if (value == null) {
      return;
    }
    if (value instanceof List<?> list) {
      list.forEach(item -> addFlat(values, item));
      return;
    }
    values.add(value);
  }

  private Object readPath(JsonNode node, String path) {
    JsonNode current = navigate(node, path);
    return jsonToJava(current);
  }

  private Object jsonToJava(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    if (node.isIntegralNumber()) {
      return node.asLong();
    }
    if (node.isFloatingPointNumber()) {
      return node.asDouble();
    }
    if (node.isBoolean()) {
      return node.asBoolean();
    }
    if (node.isTextual()) {
      return node.asText();
    }
    if (node.isArray()) {
      List<Object> values = new ArrayList<>();
      node.forEach(child -> values.add(jsonToJava(child)));
      return values;
    }
    if (node.isObject()) {
      Map<String, Object> values = new LinkedHashMap<>();
      node.properties().forEach(entry -> values.put(entry.getKey(), jsonToJava(entry.getValue())));
      return values;
    }
    return null;
  }

  private static String stripPathPrefix(String path) {
    if (path.startsWith(JSON_PATH_PREFIX)) {
      return path.substring(2);
    }
    return JSON_PATH_ROOT.equals(path) ? "" : path;
  }

  public record Result(
      String query,
      Map<String, Object> variables,
      String entryField,
      String joinPath,
      String writeTo,
      String writeMode,
      String resultKey,
      String payloadPath,
      String targetPath) {}
}
