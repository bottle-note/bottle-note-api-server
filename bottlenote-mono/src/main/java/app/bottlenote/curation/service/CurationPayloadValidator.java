package app.bottlenote.curation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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

  public record MapBackedSchema(Object value) {}
}
