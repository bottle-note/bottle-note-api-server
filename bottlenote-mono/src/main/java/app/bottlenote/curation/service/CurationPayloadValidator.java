package app.bottlenote.curation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
    if (payloadNode.isArray()) {
      if (payloadNode.isEmpty()) {
        return List.of("payload 배열은 비어 있을 수 없습니다.");
      }
      List<String> errors = new ArrayList<>();
      for (int i = 0; i < payloadNode.size(); i++) {
        errors.addAll(validateObject(schema, payloadNode.get(i), "[" + i + "]"));
      }
      return errors;
    }
    return validateObject(schema, payloadNode, "$");
  }

  private List<String> validateObject(JsonNode schema, JsonNode payload, String path) {
    if (!payload.isObject()) {
      return List.of(path + " payload는 object여야 합니다.");
    }
    JsonNode required = schema.path("required");
    if (!required.isArray()) {
      return List.of();
    }
    List<String> errors = new ArrayList<>();
    for (JsonNode field : required) {
      String fieldName = field.asText();
      if (!payload.hasNonNull(fieldName)) {
        errors.add(path + "." + fieldName + " 필드는 필수입니다.");
      }
    }
    return errors;
  }

  public record MapBackedSchema(Object value) {}
}
