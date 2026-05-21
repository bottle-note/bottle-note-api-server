package app.bottlenote.curation.service;

import static app.bottlenote.curation.exception.CurationExceptionCode.CURATION_RESPONSE_INVALID;

import app.bottlenote.curation.exception.CurationException;
import app.bottlenote.curation.service.CurationPayloadValidator.MapBackedSchema;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CurationResponseMaterializer {

  private static final String JSON_PATH_ROOT = "$";

  private final ObjectMapper objectMapper;
  private final GraphQLCurationQueryBuilder queryBuilder;
  private final GraphQLCurationExecutor graphqlExecutor;
  private final CurationPayloadValidator payloadValidator;

  public Object materialize(
      Long curationId, String specCode, Map<String, Object> responseSpec, Object payload) {
    JsonNode payloadNode = objectMapper.valueToTree(payload);
    JsonNode responseSpecNode = objectMapper.valueToTree(responseSpec);
    List<GraphQLCurationQueryBuilder.Result> queries =
        queryBuilder.build(responseSpecNode, payloadNode);

    JsonNode hydrated = payloadNode;
    for (int i = 0; i < queries.size(); i++) {
      GraphQLCurationQueryBuilder.Result query = queries.get(i);
      if (hasNoArgumentValues(query)) {
        hydrated = applyHydration(hydrated, query, emptyHydrationResult(query));
        continue;
      }

      Map<String, Object> result = graphqlExecutor.execute(curationId, i, query);
      assertNoGraphQLErrors(curationId, specCode, query, result);
      hydrated = applyHydration(hydrated, query, result);
    }

    Object materialized = objectMapper.convertValue(hydrated, Object.class);
    List<String> errors =
        payloadValidator.validate(new MapBackedSchema(responseSpec), materialized);
    if (!errors.isEmpty()) {
      log.error(
          "Curation responseSpec mismatch. curationId={}, specCode={}, errors={}",
          curationId,
          specCode,
          errors);
      throw new CurationException(CURATION_RESPONSE_INVALID);
    }
    return materialized;
  }

  private boolean hasNoArgumentValues(GraphQLCurationQueryBuilder.Result query) {
    return query.variables().values().stream().allMatch(this::isEmptyArgumentValue);
  }

  private boolean isEmptyArgumentValue(Object value) {
    if (value == null) {
      return true;
    }
    return value instanceof List<?> list && list.isEmpty();
  }

  private Map<String, Object> emptyHydrationResult(GraphQLCurationQueryBuilder.Result query) {
    return Map.of("data", Map.of(query.entryField(), List.of()));
  }

  private void assertNoGraphQLErrors(
      Long curationId,
      String specCode,
      GraphQLCurationQueryBuilder.Result query,
      Map<String, Object> result) {
    if (result == null || !(result.get("errors") instanceof List<?> errors) || errors.isEmpty()) {
      return;
    }
    log.error(
        "Curation GraphQL hydration failed. curationId={}, specCode={}, payloadPath={}, entryField={}, errors={}",
        curationId,
        specCode,
        query.payloadPath(),
        query.entryField(),
        errors);
    throw new CurationException(
        app.bottlenote.curation.exception.CurationExceptionCode.CURATION_GRAPHQL_EXECUTION_FAILED);
  }

  @SuppressWarnings("unchecked")
  private JsonNode applyHydration(
      JsonNode payload, GraphQLCurationQueryBuilder.Result query, Map<String, Object> result) {
    if (payload == null || result == null || !(result.get("data") instanceof Map<?, ?> data)) {
      return payload;
    }

    Object raw = data.get(query.entryField());
    List<?> hydrationList = normalizeHydration(raw);
    Map<Object, Map<String, Object>> byKey = indexByResultKey(hydrationList, query.resultKey());
    if (JSON_PATH_ROOT.equals(query.payloadPath())) {
      return mergeSubtree(payload, query, byKey);
    }

    JsonNode rootCopy = payload.deepCopy();
    JsonNode subtree = GraphQLCurationQueryBuilder.navigate(rootCopy, query.payloadPath());
    JsonNode merged = mergeSubtree(subtree, query, byKey);
    if (merged != null) {
      setAtPath(rootCopy, query.payloadPath(), merged);
    }
    return rootCopy;
  }

  private List<?> normalizeHydration(Object raw) {
    if (raw instanceof List<?> list) {
      return list;
    }
    if (raw instanceof Map<?, ?> map) {
      return List.of(map);
    }
    return List.of();
  }

  @SuppressWarnings("unchecked")
  private Map<Object, Map<String, Object>> indexByResultKey(List<?> list, String resultKey) {
    Map<Object, Map<String, Object>> indexed = new HashMap<>();
    for (Object item : list) {
      if (item instanceof Map<?, ?> map) {
        Object key = map.get(resultKey);
        if (key != null) {
          indexed.put(normalizeKey(key), (Map<String, Object>) map);
        }
      }
    }
    return indexed;
  }

  private JsonNode mergeSubtree(
      JsonNode subtree,
      GraphQLCurationQueryBuilder.Result query,
      Map<Object, Map<String, Object>> byKey) {
    if (subtree == null) {
      return null;
    }
    if (subtree.isArray()) {
      ArrayNode array = objectMapper.createArrayNode();
      subtree.forEach(element -> array.add(mergeElement(element, query, byKey)));
      return array;
    }
    if (subtree.isObject()) {
      return mergeElement(subtree, query, byKey);
    }
    return subtree;
  }

  private JsonNode mergeElement(
      JsonNode source,
      GraphQLCurationQueryBuilder.Result query,
      Map<Object, Map<String, Object>> byKey) {
    if (!source.isObject()) {
      return source;
    }
    ObjectNode node = ((ObjectNode) source).deepCopy();
    JsonNode joinNode = GraphQLCurationQueryBuilder.navigate(node, query.joinPath());
    if (joinNode == null || joinNode.isNull()) {
      if (query.writeTo() != null && !node.has(query.writeTo())) {
        node.set(query.writeTo(), objectMapper.nullNode());
      }
      return node;
    }

    if (query.writeTo() != null) {
      node.set(
          query.writeTo(), pickHydration(joinNode, byKey, query.writeMode(), query.resultKey()));
      return node;
    }

    Map<String, Object> hit = byKey.get(normalizeKey(jsonScalar(joinNode)));
    if (hit != null) {
      hit.forEach((key, value) -> node.set(key, objectMapper.valueToTree(value)));
    }
    return node;
  }

  private JsonNode pickHydration(
      JsonNode joinNode,
      Map<Object, Map<String, Object>> byKey,
      String writeMode,
      String resultKey) {
    if (GraphQLCurationQueryBuilder.WRITE_MODE_SINGLE.equals(writeMode)) {
      Object key = joinNode.isArray() ? firstScalar(joinNode) : jsonScalar(joinNode);
      Map<String, Object> hit = key == null ? null : byKey.get(normalizeKey(key));
      return hit == null
          ? objectMapper.nullNode()
          : objectMapper.valueToTree(withoutResultKey(hit, resultKey));
    }

    ArrayNode array = objectMapper.createArrayNode();
    if (joinNode.isArray()) {
      joinNode.forEach(value -> appendIfHit(array, byKey, jsonScalar(value), resultKey));
    } else {
      appendIfHit(array, byKey, jsonScalar(joinNode), resultKey);
    }
    return array;
  }

  private Object firstScalar(JsonNode node) {
    return node.isEmpty() ? null : jsonScalar(node.get(0));
  }

  private void appendIfHit(
      ArrayNode array, Map<Object, Map<String, Object>> byKey, Object key, String resultKey) {
    Map<String, Object> hit = byKey.get(normalizeKey(key));
    if (hit != null) {
      array.add(objectMapper.valueToTree(withoutResultKey(hit, resultKey)));
    }
  }

  private Map<String, Object> withoutResultKey(Map<String, Object> hit, String resultKey) {
    Map<String, Object> copy = new HashMap<>(hit);
    copy.remove(resultKey);
    return copy;
  }

  private void setAtPath(JsonNode root, String path, JsonNode value) {
    String trimmed = stripPathPrefix(path);
    if (trimmed.isEmpty()) {
      return;
    }
    String[] segments = trimmed.split("\\.");
    JsonNode current = root;
    for (int i = 0; i < segments.length - 1; i++) {
      current = current.get(segments[i]);
      if (current == null) {
        return;
      }
    }
    if (current instanceof ObjectNode objectNode) {
      objectNode.set(segments[segments.length - 1], value);
    }
  }

  private String stripPathPrefix(String path) {
    if (path.startsWith("$.")) {
      return path.substring(2);
    }
    return JSON_PATH_ROOT.equals(path) ? "" : path;
  }

  private Object normalizeKey(Object value) {
    if (value instanceof Number number) {
      return number.longValue();
    }
    if (value == null) {
      return null;
    }
    try {
      return Long.parseLong(value.toString());
    } catch (NumberFormatException e) {
      return value.toString();
    }
  }

  private Object jsonScalar(JsonNode node) {
    if (node.isIntegralNumber()) {
      return node.asLong();
    }
    if (node.isFloatingPointNumber()) {
      return node.asDouble();
    }
    if (node.isBoolean()) {
      return node.asBoolean();
    }
    return node.asText();
  }
}
