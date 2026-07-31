package app.bottlenote.agent.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

// 에이전트 비밀 키(UUID) 정규화 및 SHA-256 해시. 원문 키는 저장하지 않고 해시만 비교한다.
public final class AgentKeyHasher {

  private static final String ALGORITHM = "SHA-256";

  private AgentKeyHasher() {}

  /**
   * @throws IllegalArgumentException rawKey가 UUID 형식이 아닌 경우
   */
  public static String normalize(String rawKey) {
    if (rawKey == null) {
      throw new IllegalArgumentException("agentKey는 null이 될 수 없습니다.");
    }
    return UUID.fromString(rawKey.trim().toLowerCase()).toString();
  }

  public static String hash(String normalizedKey) {
    try {
      MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
      return HexFormat.of()
          .formatHex(digest.digest(normalizedKey.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(ALGORITHM + " 알고리즘을 사용할 수 없습니다.", e);
    }
  }

  /**
   * @throws IllegalArgumentException rawKey가 UUID 형식이 아닌 경우
   */
  public static String normalizeAndHash(String rawKey) {
    return hash(normalize(rawKey));
  }
}
