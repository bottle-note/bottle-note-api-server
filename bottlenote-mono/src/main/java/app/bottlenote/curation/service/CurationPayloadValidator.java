package app.bottlenote.curation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class CurationPayloadValidator {

  private static final int MAX_PAYLOAD_BYTES = 128 * 1024;

  private final ObjectMapper objectMapper;

  public CurationPayloadValidator(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public List<String> validate(MapBackedSchema requestSpec, Object payload) {
    JsonNode schema = objectMapper.valueToTree(requestSpec.value());
    JsonNode payloadNode = objectMapper.valueToTree(payload);

    if (payloadNode == null || payloadNode.isNull()) {
      return List.of("payload가 null입니다.");
    }
    int payloadBytes = payloadNode.toString().getBytes(StandardCharsets.UTF_8).length;
    if (payloadBytes > MAX_PAYLOAD_BYTES) {
      return List.of("payload size must be <= " + MAX_PAYLOAD_BYTES + ", actual=" + payloadBytes);
    }

    if ("array".equals(schema.path("x-container").asText())) {
      if (!payloadNode.isArray()) {
        return List.of("$ payload는 array여야 합니다.");
      }
      if (payloadNode.isEmpty()) {
        return List.of("payload 배열은 비어 있을 수 없습니다.");
      }
      List<String> errors = new ArrayList<>();
      for (int i = 0; i < payloadNode.size(); i++) {
        validateNode(schema, payloadNode.get(i), "$[" + i + "]", true, errors);
      }
      return errors;
    }

    List<String> errors = new ArrayList<>();
    validateNode(schema, payloadNode, "$", true, errors);
    return errors;
  }

  public List<String> validateSpec(String rootPath, MapBackedSchema spec) {
    JsonNode schema = objectMapper.valueToTree(spec.value());
    List<String> errors = new ArrayList<>();
    validateSpecNode(rootPath, schema, new ArrayDeque<>(), Set.of(), null, errors);
    return errors;
  }

  private void validateSpecNode(
      String rootPath,
      JsonNode schema,
      ArrayDeque<String> path,
      Set<String> siblingKeys,
      JsonNode siblingProperties,
      List<String> errors) {
    JsonNode dependsOn = schema.get("x-depends-on");
    if (dependsOn != null) {
      validateDependsOn(rootPath, path, siblingKeys, siblingProperties, dependsOn, errors);
    }

    JsonNode properties = schema.path("properties");
    if (properties.isObject()) {
      validateNoDependencyCycle(rootPath, path, properties, errors);
      Set<String> childSiblingKeys = new HashSet<>();
      properties.fieldNames().forEachRemaining(childSiblingKeys::add);
      for (Map.Entry<String, JsonNode> property : properties.properties()) {
        path.addLast(property.getKey());
        validateSpecNode(rootPath, property.getValue(), path, childSiblingKeys, properties, errors);
        path.removeLast();
      }
    }

    JsonNode items = schema.path("items");
    if (items.isObject()) {
      if (!path.isEmpty()) {
        String arrayField = path.removeLast();
        path.addLast(arrayField + "[]");
      }
      validateSpecNode(rootPath, items, path, siblingKeys, siblingProperties, errors);
      if (!path.isEmpty() && path.peekLast().endsWith("[]")) {
        String arrayField = path.removeLast();
        path.addLast(arrayField.substring(0, arrayField.length() - 2));
      }
    }
  }

  private void validateDependsOn(
      String rootPath,
      ArrayDeque<String> path,
      Set<String> siblingKeys,
      JsonNode siblingProperties,
      JsonNode dependsOn,
      List<String> errors) {
    String fieldPath = fieldPath(rootPath, path);
    if (!dependsOn.isArray()) {
      errors.add(fieldPath + " x-depends-on은 배열이어야 합니다.");
      return;
    }

    Set<String> seenDependencies = new HashSet<>();
    for (JsonNode dependency : dependsOn) {
      if (!dependency.isObject()) {
        errors.add(fieldPath + " x-depends-on 항목은 object여야 합니다.");
        continue;
      }
      String key = textualValue(dependency, "key");
      String value = textualValue(dependency, "value");
      String description = textualValue(dependency, "description");
      if (key == null) {
        errors.add(fieldPath + " x-depends-on.key는 필수 string입니다.");
      }
      if (value == null) {
        errors.add(fieldPath + " x-depends-on.value는 필수 string입니다.");
      }
      if (description == null || description.isBlank()) {
        errors.add(fieldPath + " x-depends-on.description은 비어있을 수 없습니다.");
      }
      if (key == null || value == null) {
        continue;
      }
      if (!siblingKeys.contains(key)) {
        errors.add(fieldPath + " 의존 대상 key가 같은 scope에 없습니다: " + key);
        continue;
      }
      if (key.equals(path.peekLast())) {
        errors.add(fieldPath + " 자기 자신을 x-depends-on으로 참조할 수 없습니다: " + key);
        continue;
      }
      if (!seenDependencies.add(key + "=" + value)) {
        errors.add(fieldPath + " 중복 x-depends-on dependency입니다: " + key + "=" + value);
        continue;
      }

      JsonNode targetSchema = siblingProperties.get(key);
      if (!canNormalizeDependencyValue(value, targetSchema)) {
        errors.add(fieldPath + " value를 의존 대상 필드 타입으로 정규화할 수 없습니다: " + key + "=" + value);
      }
      if (!enumContainsTextValue(value, targetSchema)) {
        errors.add(fieldPath + " value가 의존 대상 enum에 없습니다: " + key + "=" + value);
      }
    }
  }

  private void validateNoDependencyCycle(
      String rootPath, ArrayDeque<String> path, JsonNode siblingProperties, List<String> errors) {
    Map<String, List<String>> graph = new HashMap<>();
    siblingProperties
        .properties()
        .forEach(
            property -> {
              JsonNode dependsOn = property.getValue().path("x-depends-on");
              if (dependsOn.isArray()) {
                List<String> dependencyKeys = new ArrayList<>();
                for (JsonNode dependency : dependsOn) {
                  JsonNode key = dependency.path("key");
                  if (key.isTextual()) {
                    dependencyKeys.add(key.asText());
                  }
                }
                graph.put(property.getKey(), dependencyKeys);
              }
            });

    Set<String> visiting = new HashSet<>();
    Set<String> visited = new HashSet<>();
    for (String field : graph.keySet()) {
      if (hasCycle(field, graph, visiting, visited)) {
        errors.add(fieldPath(rootPath, path) + " x-depends-on 순환 참조가 있습니다: " + field);
      }
    }
  }

  private boolean hasCycle(
      String field, Map<String, List<String>> graph, Set<String> visiting, Set<String> visited) {
    if (visited.contains(field)) {
      return false;
    }
    if (!visiting.add(field)) {
      return true;
    }
    for (String dependency : graph.getOrDefault(field, List.of())) {
      if (graph.containsKey(dependency) && hasCycle(dependency, graph, visiting, visited)) {
        return true;
      }
    }
    visiting.remove(field);
    visited.add(field);
    return false;
  }

  private void validateNode(
      JsonNode schema, JsonNode payload, String path, boolean required, List<String> errors) {
    if (payload == null || payload.isMissingNode()) {
      if (required) {
        errors.add(path + " 필드는 필수입니다.");
      }
      return;
    }

    boolean nullable = schema.path("nullable").asBoolean(false);
    if (payload.isNull()) {
      if (required && !nullable) {
        errors.add(path + " 필드는 null일 수 없습니다.");
      }
      return;
    }

    String type = schema.path("type").asText("");
    if (!matchesType(type, payload)) {
      errors.add(path + " 타입이 " + type + "이어야 합니다.");
      return;
    }

    validateEnum(schema, payload, path, errors);
    validateString(schema, payload, path, errors);
    validateNumber(schema, payload, path, errors);

    if (payload.isObject()) {
      validateObject(schema, payload, path, errors);
    }
    if (payload.isArray()) {
      validateArray(schema, payload, path, errors);
    }
  }

  private void validateObject(JsonNode schema, JsonNode payload, String path, List<String> errors) {
    JsonNode requiredFields = schema.path("required");
    JsonNode properties = schema.path("properties");

    if (requiredFields.isArray()) {
      for (JsonNode field : requiredFields) {
        String fieldName = field.asText();
        JsonNode value = payload.get(fieldName);
        if (value == null || value.isNull()) {
          errors.add(path + "." + fieldName + " 필드는 필수입니다.");
        }
      }
    }

    if (!properties.isObject()) {
      return;
    }

    Iterator<String> names = properties.fieldNames();
    while (names.hasNext()) {
      String name = names.next();
      JsonNode value = payload.get(name);
      if (value == null) {
        continue;
      }
      validateNode(properties.get(name), value, path + "." + name, false, errors);
    }
  }

  private void validateArray(JsonNode schema, JsonNode payload, String path, List<String> errors) {
    if (schema.has("minItems") && payload.size() < schema.get("minItems").asInt()) {
      errors.add(path + " 배열 크기는 최소 " + schema.get("minItems").asInt() + "개여야 합니다.");
    }
    if (schema.has("maxItems") && payload.size() > schema.get("maxItems").asInt()) {
      errors.add(path + " 배열 크기는 최대 " + schema.get("maxItems").asInt() + "개여야 합니다.");
    }

    JsonNode itemSchema = schema.path("items");
    if (!itemSchema.isObject()) {
      return;
    }
    for (int i = 0; i < payload.size(); i++) {
      validateNode(itemSchema, payload.get(i), path + "[" + i + "]", true, errors);
    }
  }

  private void validateEnum(JsonNode schema, JsonNode payload, String path, List<String> errors) {
    JsonNode enumValues = schema.path("enum");
    if (!enumValues.isArray()) {
      return;
    }
    for (JsonNode enumValue : enumValues) {
      if (enumValue.equals(payload)) {
        return;
      }
    }
    errors.add(path + " 값이 허용된 enum이 아닙니다.");
  }

  private void validateString(JsonNode schema, JsonNode payload, String path, List<String> errors) {
    if (!payload.isTextual()) {
      return;
    }
    String value = payload.asText();
    if (schema.has("minLength") && value.length() < schema.get("minLength").asInt()) {
      errors.add(path + " 문자열 길이는 최소 " + schema.get("minLength").asInt() + "자여야 합니다.");
    }
    if (schema.has("maxLength") && value.length() > schema.get("maxLength").asInt()) {
      errors.add(path + " 문자열 길이는 최대 " + schema.get("maxLength").asInt() + "자여야 합니다.");
    }
  }

  private void validateNumber(JsonNode schema, JsonNode payload, String path, List<String> errors) {
    if (!payload.isNumber()) {
      return;
    }
    BigDecimal value = payload.decimalValue();
    if (schema.has("minimum") && value.compareTo(schema.get("minimum").decimalValue()) < 0) {
      errors.add(path + " 값은 최소 " + schema.get("minimum").asText() + " 이상이어야 합니다.");
    }
    if (schema.has("maximum") && value.compareTo(schema.get("maximum").decimalValue()) > 0) {
      errors.add(path + " 값은 최대 " + schema.get("maximum").asText() + " 이하여야 합니다.");
    }
  }

  private boolean matchesType(String type, JsonNode payload) {
    return switch (type) {
      case "", "any" -> true;
      case "object" -> payload.isObject();
      case "array" -> payload.isArray();
      case "string" -> payload.isTextual();
      case "integer" -> payload.isIntegralNumber();
      case "number" -> payload.isNumber();
      case "boolean" -> payload.isBoolean();
      default -> true;
    };
  }

  private String textualValue(JsonNode node, String fieldName) {
    JsonNode value = node.path(fieldName);
    return value.isTextual() ? value.asText() : null;
  }

  private boolean canNormalizeDependencyValue(String value, JsonNode targetSchema) {
    return switch (targetSchema.path("type").asText("string")) {
      case "boolean" -> "true".equals(value) || "false".equals(value);
      case "integer" -> value.matches("-?\\d+");
      case "number" -> value.matches("-?(\\d+)(\\.\\d+)?");
      default -> true;
    };
  }

  private boolean enumContainsTextValue(String value, JsonNode targetSchema) {
    JsonNode enumValues = targetSchema.path("enum");
    if (!enumValues.isArray()) {
      return true;
    }
    for (JsonNode enumValue : enumValues) {
      if (value.equals(enumValue.asText())) {
        return true;
      }
    }
    return false;
  }

  private String fieldPath(String rootPath, ArrayDeque<String> path) {
    if (path.isEmpty()) {
      return rootPath;
    }
    return rootPath + "." + String.join(".", path);
  }

  public record MapBackedSchema(Object value) {}
}
