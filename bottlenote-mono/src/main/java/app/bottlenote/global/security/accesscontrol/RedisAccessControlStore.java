package app.bottlenote.global.security.accesscontrol;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.SessionCallback;
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

  /** store 소유 전용 factory(비등록). null이면 destroy 시 정리하지 않는다. */
  private final LettuceConnectionFactory ownedConnectionFactory;

  public RedisAccessControlStore(StringRedisTemplate redisTemplate) {
    this(redisTemplate, null);
  }

  public RedisAccessControlStore(
      StringRedisTemplate redisTemplate, LettuceConnectionFactory ownedConnectionFactory) {
    this.redisTemplate = redisTemplate;
    this.ownedConnectionFactory = ownedConnectionFactory;
  }

  /** Spring {@code destroyMethod} — 소유 factory 자원 정리. */
  public void destroy() {
    if (ownedConnectionFactory != null) {
      ownedConnectionFactory.destroy();
    }
  }

  @Override
  public boolean isBanned(String ip) {
    Boolean exists = redisTemplate.hasKey(BAN_KEY_PREFIX + ip);
    return Boolean.TRUE.equals(exists);
  }

  @Override
  public void ban(String ip, Duration ttl, String reason) {
    String banKey = BAN_KEY_PREFIX + ip;
    String reasonKey = BAN_REASON_PREFIX + ip;
    redisTemplate.executePipelined(
        new SessionCallback<>() {
          @Override
          @SuppressWarnings("unchecked")
          public Object execute(RedisOperations operations) {
            operations.opsForValue().set(banKey, "1", ttl);
            if (reason != null && !reason.isBlank()) {
              operations.opsForValue().set(reasonKey, reason, ttl);
            } else {
              operations.delete(reasonKey);
            }
            return null;
          }
        });
  }

  @Override
  public void unban(String ip) {
    redisTemplate.delete(List.of(BAN_KEY_PREFIX + ip, BAN_REASON_PREFIX + ip));
  }

  @Override
  public BanInfo getBan(String ip) {
    String banKey = BAN_KEY_PREFIX + ip;
    String reasonKey = BAN_REASON_PREFIX + ip;
    List<Object> pipelined =
        redisTemplate.executePipelined(
            new SessionCallback<>() {
              @Override
              @SuppressWarnings("unchecked")
              public Object execute(RedisOperations operations) {
                operations.hasKey(banKey);
                operations.getExpire(banKey, TimeUnit.SECONDS);
                operations.opsForValue().get(reasonKey);
                return null;
              }
            });
    if (pipelined == null || pipelined.size() < 3) {
      return null;
    }
    if (!Boolean.TRUE.equals(pipelined.get(0))) {
      return null;
    }
    Long expire = pipelined.get(1) instanceof Long ttl ? ttl : null;
    String reason = pipelined.get(2) instanceof String value ? value : null;
    return new BanInfo(ip, reason == null ? "" : reason, expire == null ? -1 : expire);
  }

  /**
   * 활성 ban 목록. SCAN으로 키를 모은 뒤 TTL/reason을 pipeline 1회 왕복으로 조회한다. 항목마다 EXISTS·TTL·GET을 순차 호출하지 않는다.
   */
  @Override
  public List<BanInfo> listBans(int max) {
    int limit = Math.max(max, 0);
    List<String> banKeys = new ArrayList<>();
    if (limit == 0) {
      return List.of();
    }
    ScanOptions options = ScanOptions.scanOptions().match(BAN_KEY_PREFIX + "*").count(100).build();
    try (Cursor<String> cursor = redisTemplate.scan(options)) {
      while (cursor.hasNext() && banKeys.size() < limit) {
        String key = cursor.next();
        if (key == null || !key.startsWith(BAN_KEY_PREFIX)) {
          continue;
        }
        banKeys.add(key);
      }
    }
    if (banKeys.isEmpty()) {
      return List.of();
    }

    List<Object> pipelined =
        redisTemplate.executePipelined(
            new SessionCallback<>() {
              @Override
              @SuppressWarnings("unchecked")
              public Object execute(RedisOperations operations) {
                for (String banKey : banKeys) {
                  operations.getExpire(banKey, TimeUnit.SECONDS);
                  String ip = banKey.substring(BAN_KEY_PREFIX.length());
                  operations.opsForValue().get(BAN_REASON_PREFIX + ip);
                }
                return null;
              }
            });

    List<BanInfo> result = new ArrayList<>(banKeys.size());
    if (pipelined == null) {
      return result;
    }
    for (int i = 0; i < banKeys.size(); i++) {
      int base = i * 2;
      if (base + 1 >= pipelined.size()) {
        break;
      }
      Long ttlSeconds = pipelined.get(base) instanceof Long ttl ? ttl : null;
      // -2: key missing (만료 레이스), -1: no expire, >=0: remaining seconds
      if (ttlSeconds == null || ttlSeconds == -2L) {
        continue;
      }
      String ip = banKeys.get(i).substring(BAN_KEY_PREFIX.length());
      String reason = pipelined.get(base + 1) instanceof String value ? value : "";
      result.add(new BanInfo(ip, reason == null ? "" : reason, ttlSeconds));
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
