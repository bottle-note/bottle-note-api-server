package app.bottlenote.observability.visitor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@Slf4j
public final class RedisVisitorIdentityResolver implements VisitorIdentityResolver {
  private static final DefaultRedisScript<String> RESOLVE_SCRIPT =
      new DefaultRedisScript<>(
          """
      local existing = redis.call('GET', KEYS[1])
      if existing then return existing end
      redis.call('SET', KEYS[1], ARGV[1], 'PX', 3000)
      return ARGV[1]
      """,
          String.class);
  private final StringRedisTemplate redisTemplate;

  public RedisVisitorIdentityResolver(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @Override
  public String resolve(String clientIp, String userAgent) {
    String candidate = UUID.randomUUID().toString();
    if (clientIp == null || clientIp.isBlank() || userAgent == null || userAgent.isBlank()) {
      return candidate;
    }
    try {
      String resolved =
          redisTemplate.execute(RESOLVE_SCRIPT, List.of(key(clientIp, userAgent)), candidate);
      if (resolved != null && UUID.fromString(resolved).toString().equals(resolved)) {
        return resolved;
      }
      log.warn("Visitor identity bootstrap returned an invalid result; using a new identity");
    } catch (RuntimeException exception) {
      // 예외 메시지에 Redis 키나 연결 인증정보가 섞이지 않도록 종류만 기록한다.
      log.warn(
          "Visitor identity bootstrap unavailable ({}); using a new identity",
          exception.getClass().getSimpleName());
    }
    return candidate;
  }

  static String key(String clientIp, String userAgent) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(clientIp.trim().getBytes(StandardCharsets.UTF_8));
      digest.update((byte) 0);
      return "visitor-identity:bootstrap:v1:"
          + HexFormat.of().formatHex(digest.digest(userAgent.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }
}
