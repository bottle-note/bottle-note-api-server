package app.bottlenote.agent.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

// 에이전트 비밀 API Key의 형식을 검증하고 SHA-256 해시만 비교한다.
public final class AgentKeyHasher {

  private static final String ALGORITHM = "SHA-256";
  private static final Pattern API_KEY_PATTERN = Pattern.compile("^bn_agent_[A-Za-z0-9_-]{43}$");

  private AgentKeyHasher() {}

  /**
   * @throws IllegalArgumentException rawKey가 에이전트 API Key 형식이 아닌 경우
   */
  public static String validate(String rawKey) {
    if (rawKey == null || !API_KEY_PATTERN.matcher(rawKey).matches()) {
      throw new IllegalArgumentException("agentKey 형식이 올바르지 않습니다.");
    }
    return rawKey;
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
   * @throws IllegalArgumentException rawKey가 에이전트 API Key 형식이 아닌 경우
   */
  public static String validateAndHash(String rawKey) {
    return hash(validate(rawKey));
  }
}
