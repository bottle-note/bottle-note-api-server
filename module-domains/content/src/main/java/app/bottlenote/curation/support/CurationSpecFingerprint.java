package app.bottlenote.curation.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

// 스펙 내용 비교용 지문. 저장 왕복에서 키 순서·공백·수치 표기가 흔들리므로 정규화 후 해시한다.
@Component
public final class CurationSpecFingerprint {

  private static final String ALGORITHM = "SHA-256";

  private final ObjectMapper objectMapper;

  public CurationSpecFingerprint(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public String of(Map<String, Object> spec) {
    JsonNode canonical = canonicalize(objectMapper.valueToTree(spec));
    return digest(canonical == null ? "" : canonical.toString());
  }

  public boolean isSame(Map<String, Object> left, Map<String, Object> right) {
    return of(left).equals(of(right));
  }

  // 객체 키를 재귀 정렬한다. MySQL JSON 컬럼이 키를 자체 정렬해 저장 전후 순서가 달라진다.
  private JsonNode canonicalize(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    if (node.isObject()) {
      List<String> names = new ArrayList<>();
      node.fieldNames().forEachRemaining(names::add);
      names.sort(String::compareTo);
      ObjectNode sorted = objectMapper.createObjectNode();
      for (String name : names) {
        JsonNode child = canonicalize(node.get(name));
        sorted.set(name, child == null ? objectMapper.nullNode() : child);
      }
      return sorted;
    }
    if (node.isArray()) {
      // 배열은 순서가 의미를 가지므로 정렬하지 않는다.
      ArrayNode canonical = objectMapper.createArrayNode();
      node.forEach(
          child -> {
            JsonNode value = canonicalize(child);
            canonical.add(value == null ? objectMapper.nullNode() : value);
          });
      return canonical;
    }
    // 1 과 1.0, 1.2 와 1.20 이 왕복 과정에서 갈리므로 실수는 항상 trailing zero를 떨어낸다.
    if (node.isFloatingPointNumber()) {
      BigDecimal stripped = node.decimalValue().stripTrailingZeros();
      return stripped.scale() <= 0
          ? objectMapper.getNodeFactory().numberNode(stripped.toBigInteger())
          : objectMapper.getNodeFactory().numberNode(stripped);
    }
    return node;
  }

  private String digest(String canonical) {
    try {
      MessageDigest messageDigest = MessageDigest.getInstance(ALGORITHM);
      return HexFormat.of()
          .formatHex(messageDigest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(ALGORITHM + " 알고리즘을 사용할 수 없습니다.", e);
    }
  }
}
