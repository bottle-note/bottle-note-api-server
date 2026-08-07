package app.bottlenote.global.security.accesscontrol;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

public class RedisAccessControlStore implements AccessControlStore {

  private static final String BAN_KEY_PREFIX = "bn:ac:ban:";
  private static final String BAN_REASON_PREFIX = "bn:ac:ban-reason:";
  private static final String RATE_KEY_PREFIX = "bn:ac:rl:";

  /**
   * INCR + 최초 생성 시 EXPIRE를 원자적으로 수행한다.
   *
   * <pre>
   * KEYS[1]=rate key, ARGV[1]=window seconds
   * return current count
   * </pre>
   */
  private static final DefaultRedisScript<Long> TRY_CONSUME_SCRIPT = new DefaultRedisScript<>();

  static {
    TRY_CONSUME_SCRIPT.setResultType(Long.class);
    TRY_CONSUME_SCRIPT.setScriptText(
        """
        local current = redis.call('INCR', KEYS[1])
        if current == 1 then
          redis.call('EXPIRE', KEYS[1], ARGV[1])
        end
        return current
        """);
  }

  private final StringRedisTemplate redisTemplate;

  public RedisAccessControlStore(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @Override
  public boolean isBanned(String ip) {
    Boolean exists = redisTemplate.hasKey(BAN_KEY_PREFIX + ip);
    return Boolean.TRUE.equals(exists);
  }

  @Override
  public void ban(String ip, Duration ttl, String reason) {
    String banKey = BAN_KEY_PREFIX + ip;
    redisTemplate.opsForValue().set(banKey, "1", ttl);
    if (reason != null && !reason.isBlank()) {
      redisTemplate.opsForValue().set(BAN_REASON_PREFIX + ip, reason, ttl);
    } else {
      redisTemplate.delete(BAN_REASON_PREFIX + ip);
    }
  }

  @Override
  public void unban(String ip) {
    redisTemplate.delete(BAN_KEY_PREFIX + ip);
    redisTemplate.delete(BAN_REASON_PREFIX + ip);
  }

  @Override
  public BanInfo getBan(String ip) {
    if (!isBanned(ip)) {
      return null;
    }
    Long expire = redisTemplate.getExpire(BAN_KEY_PREFIX + ip, TimeUnit.SECONDS);
    String reason = redisTemplate.opsForValue().get(BAN_REASON_PREFIX + ip);
    return new BanInfo(ip, reason == null ? "" : reason, expire == null ? -1 : expire);
  }

  @Override
  public long tryConsume(String key, int limit, Duration window) {
    String redisKey = RATE_KEY_PREFIX + key;
    List<String> keys = Collections.singletonList(redisKey);
    Long count =
        redisTemplate.execute(TRY_CONSUME_SCRIPT, keys, String.valueOf(window.getSeconds()));
    if (count == null) {
      throw new IllegalStateException("redis tryConsume script returned null");
    }
    if (count > limit) {
      return -1;
    }
    return limit - count;
  }
}
