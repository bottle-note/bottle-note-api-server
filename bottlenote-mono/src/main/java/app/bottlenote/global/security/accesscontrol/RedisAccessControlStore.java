package app.bottlenote.global.security.accesscontrol;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

public class RedisAccessControlStore implements AccessControlStore {

  private static final String BAN_KEY_PREFIX = "bn:ac:ban:";
  private static final String BAN_REASON_PREFIX = "bn:ac:ban-reason:";
  private static final String RATE_KEY_PREFIX = "bn:ac:rl:";

  /** KEYS[1]=rate key, ARGV[1]=window seconds returns {count, pttl_ms} */
  private static final DefaultRedisScript<List> TRY_CONSUME_SCRIPT = new DefaultRedisScript<>();

  static {
    TRY_CONSUME_SCRIPT.setResultType(List.class);
    TRY_CONSUME_SCRIPT.setScriptText(
        """
        local current = redis.call('INCR', KEYS[1])
        if current == 1 then
          redis.call('EXPIRE', KEYS[1], ARGV[1])
        end
        local pttl = redis.call('PTTL', KEYS[1])
        return {current, pttl}
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
  public List<BanInfo> listBans(int max) {
    int limit = Math.max(max, 0);
    List<BanInfo> result = new ArrayList<>();
    if (limit == 0) {
      return result;
    }
    ScanOptions options = ScanOptions.scanOptions().match(BAN_KEY_PREFIX + "*").count(100).build();
    try (Cursor<String> cursor = redisTemplate.scan(options)) {
      while (cursor.hasNext() && result.size() < limit) {
        String key = cursor.next();
        if (key == null || !key.startsWith(BAN_KEY_PREFIX)) {
          continue;
        }
        String ip = key.substring(BAN_KEY_PREFIX.length());
        BanInfo info = getBan(ip);
        if (info != null) {
          result.add(info);
        }
      }
    }
    return result;
  }

  @Override
  @SuppressWarnings("unchecked")
  public ConsumeResult tryConsume(String key, int limit, Duration window) {
    String redisKey = RATE_KEY_PREFIX + key;
    List<String> keys = Collections.singletonList(redisKey);
    List<Long> raw =
        redisTemplate.execute(TRY_CONSUME_SCRIPT, keys, String.valueOf(window.getSeconds()));
    if (raw == null || raw.size() < 2 || raw.get(0) == null) {
      throw new IllegalStateException("redis tryConsume script returned null");
    }
    long count = raw.get(0);
    long pttlMs = raw.get(1) == null ? -1L : raw.get(1);
    long retryAfterSeconds = pttlMs > 0 ? Math.max(1, (pttlMs + 999) / 1000) : window.getSeconds();
    if (count > limit) {
      return ConsumeResult.deny(retryAfterSeconds);
    }
    return ConsumeResult.allow(limit - count);
  }
}
