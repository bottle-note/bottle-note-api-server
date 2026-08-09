package app.bottlenote.global.security.accesscontrol;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

public class RedisAccessControlStore implements AccessControlStore {

  private static final String BAN_KEY_PREFIX = "bn:ac:ban:";
  private static final String BAN_REASON_PREFIX = "bn:ac:ban-reason:";
  private static final String BAN_VERSION_PREFIX = "bn:ac:ban-version:";
  private static final String RATE_KEY_PREFIX = "bn:ac:rl:";
  private static final long PROJECTION_VERSION_TTL_SECONDS = Duration.ofDays(180).toSeconds();
  private static final int LEGACY_SCAN_CANDIDATE_LIMIT = 100;

  /** KEYS[1]=rate key, ARGV[1]=window seconds returns {count, pttl_ms} */
  private static final DefaultRedisScript<List> TRY_CONSUME_SCRIPT = new DefaultRedisScript<>();

  /** KEYS are hash-tagged with the IP so Redis Cluster can execute atomically. */
  private static final DefaultRedisScript<Long> PROJECT_BAN_SCRIPT = new DefaultRedisScript<>();

  private static final DefaultRedisScript<Long> PROJECT_UNBAN_SCRIPT = new DefaultRedisScript<>();

  private static final DefaultRedisScript<Long> REMOVE_UNVERSIONED_BAN_SCRIPT =
      new DefaultRedisScript<>();

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
    PROJECT_BAN_SCRIPT.setResultType(Long.class);
    PROJECT_BAN_SCRIPT.setScriptText(
        """
        local current = redis.call('GET', KEYS[3])
        if current and tonumber(ARGV[1]) < tonumber(current) then return 0 end
        redis.call('SET', KEYS[1], '1', 'EX', ARGV[2])
        if ARGV[3] == '' then redis.call('DEL', KEYS[2])
        else redis.call('SET', KEYS[2], ARGV[3], 'EX', ARGV[2]) end
        redis.call('SET', KEYS[3], ARGV[1], 'EX', ARGV[4])
        return 1
        """);
    PROJECT_UNBAN_SCRIPT.setResultType(Long.class);
    PROJECT_UNBAN_SCRIPT.setScriptText(
        """
        local current = redis.call('GET', KEYS[3])
        if current and tonumber(ARGV[1]) < tonumber(current) then return 0 end
        redis.call('DEL', KEYS[1], KEYS[2])
        redis.call('SET', KEYS[3], ARGV[1], 'EX', ARGV[2])
        return 1
        """);
    REMOVE_UNVERSIONED_BAN_SCRIPT.setResultType(Long.class);
    REMOVE_UNVERSIONED_BAN_SCRIPT.setScriptText(
        """
        if redis.call('EXISTS', KEYS[3]) == 1 then return 0 end
        redis.call('DEL', KEYS[1], KEYS[2])
        return 1
        """);
  }

  private final StringRedisTemplate redisTemplate;

  private final List<StringRedisTemplate> rateLimitTemplates;
  private final List<LettuceConnectionFactory> ownedConnectionFactories;
  private final AtomicLong nextRateLimitTemplateIndex = new AtomicLong();

  public RedisAccessControlStore(StringRedisTemplate redisTemplate) {
    this(redisTemplate, null);
  }

  public RedisAccessControlStore(
      StringRedisTemplate redisTemplate, LettuceConnectionFactory ownedConnectionFactory) {
    this(
        redisTemplate,
        List.of(redisTemplate),
        ownedConnectionFactory == null ? List.of() : List.of(ownedConnectionFactory));
  }

  RedisAccessControlStore(
      StringRedisTemplate redisTemplate,
      List<StringRedisTemplate> rateLimitTemplates,
      List<LettuceConnectionFactory> ownedConnectionFactories) {
    this.redisTemplate = redisTemplate;
    if (rateLimitTemplates == null || rateLimitTemplates.isEmpty()) {
      throw new IllegalArgumentException("rateLimitTemplates must not be empty");
    }
    this.rateLimitTemplates = List.copyOf(rateLimitTemplates);
    this.ownedConnectionFactories = List.copyOf(ownedConnectionFactories);
  }

  /** Spring {@code destroyMethod} — 소유 factory 자원 정리. */
  public void destroy() {
    RuntimeException failure = null;
    for (int index = ownedConnectionFactories.size() - 1; index >= 0; index--) {
      try {
        ownedConnectionFactories.get(index).destroy();
      } catch (RuntimeException exception) {
        if (failure == null) {
          failure = exception;
        } else {
          failure.addSuppressed(exception);
        }
      }
    }
    if (failure != null) {
      throw failure;
    }
  }

  @Override
  public BanLookup lookupBan(String ip) {
    return executeRedis(
        () ->
            redisTemplate.execute(
                (RedisCallback<BanLookup>)
                    connection -> {
                      Long projectedTtlMillis =
                          connection.keyCommands().pTtl(serializeKey(banKey(ip)));
                      if (projectedTtlMillis == null) {
                        throw unavailablePttl();
                      }
                      if (projectedTtlMillis != -2L) {
                        return new BanLookup(true, toTtlSeconds(projectedTtlMillis));
                      }
                      Long legacyTtlMillis =
                          connection.keyCommands().pTtl(serializeKey(legacyBanKey(ip)));
                      if (legacyTtlMillis == null) {
                        throw unavailablePttl();
                      }
                      return legacyTtlMillis == -2L
                          ? BanLookup.notBanned()
                          : new BanLookup(true, toTtlSeconds(legacyTtlMillis));
                    }));
  }

  @Override
  public boolean isBanned(String ip) {
    return lookupBan(ip).banned();
  }

  @Override
  public void ban(String ip, Duration ttl, String reason) {
    String banKey = banKey(ip);
    String reasonKey = reasonKey(ip);
    String legacyBanKey = legacyBanKey(ip);
    String legacyReasonKey = legacyReasonKey(ip);
    redisTemplate.executePipelined(
        new SessionCallback<>() {
          @Override
          @SuppressWarnings("unchecked")
          public Object execute(RedisOperations operations) {
            operations.opsForValue().set(banKey, "1", ttl);
            operations.opsForValue().set(legacyBanKey, "1", ttl);
            if (reason != null && !reason.isBlank()) {
              operations.opsForValue().set(reasonKey, reason, ttl);
              operations.opsForValue().set(legacyReasonKey, reason, ttl);
            } else {
              operations.delete(reasonKey);
              operations.delete(legacyReasonKey);
            }
            return null;
          }
        });
  }

  @Override
  public void unban(String ip) {
    redisTemplate.delete(List.of(banKey(ip), reasonKey(ip), legacyBanKey(ip), legacyReasonKey(ip)));
  }

  @Override
  public void projectBan(String ip, Duration ttl, String reason, long eventId) {
    redisTemplate.execute(
        PROJECT_BAN_SCRIPT,
        List.of(banKey(ip), reasonKey(ip), versionKey(ip)),
        String.valueOf(eventId),
        String.valueOf(Math.max(1, ttl.toSeconds())),
        reason == null ? "" : reason,
        String.valueOf(PROJECTION_VERSION_TTL_SECONDS));
    redisTemplate.delete(List.of(legacyBanKey(ip), legacyReasonKey(ip)));
  }

  @Override
  public void projectUnban(String ip, long eventId) {
    Long applied =
        redisTemplate.execute(
            PROJECT_UNBAN_SCRIPT,
            List.of(banKey(ip), reasonKey(ip), versionKey(ip)),
            String.valueOf(eventId),
            String.valueOf(PROJECTION_VERSION_TTL_SECONDS));
    if (Long.valueOf(1L).equals(applied)) {
      redisTemplate.delete(List.of(legacyBanKey(ip), legacyReasonKey(ip)));
    }
  }

  @Override
  public void removeUnversionedBan(String ip) {
    Long removed =
        redisTemplate.execute(
            REMOVE_UNVERSIONED_BAN_SCRIPT, List.of(banKey(ip), reasonKey(ip), versionKey(ip)));
    if (Long.valueOf(1L).equals(removed)) {
      redisTemplate.delete(List.of(legacyBanKey(ip), legacyReasonKey(ip)));
    }
  }

  @Override
  public BanInfo getBan(String ip) {
    String banKey = banKey(ip);
    String reasonKey = reasonKey(ip);
    String legacyBanKey = legacyBanKey(ip);
    String legacyReasonKey = legacyReasonKey(ip);
    List<Object> pipelined =
        redisTemplate.executePipelined(
            new SessionCallback<>() {
              @Override
              @SuppressWarnings("unchecked")
              public Object execute(RedisOperations operations) {
                operations.hasKey(banKey);
                operations.getExpire(banKey, TimeUnit.SECONDS);
                operations.opsForValue().get(reasonKey);
                operations.hasKey(legacyBanKey);
                operations.getExpire(legacyBanKey, TimeUnit.SECONDS);
                operations.opsForValue().get(legacyReasonKey);
                return null;
              }
            });
    if (pipelined == null || pipelined.size() < 6) {
      return null;
    }
    if (!Boolean.TRUE.equals(pipelined.get(0))) {
      if (!Boolean.TRUE.equals(pipelined.get(3))) {
        return null;
      }
      Long legacyExpire = pipelined.get(4) instanceof Long ttl ? ttl : null;
      String legacyReason = pipelined.get(5) instanceof String value ? value : null;
      return new BanInfo(
          ip, legacyReason == null ? "" : legacyReason, legacyExpire == null ? -1 : legacyExpire);
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
    Map<String, String> banKeysByIp = new LinkedHashMap<>();
    if (limit == 0) {
      return List.of();
    }
    ScanOptions options = ScanOptions.scanOptions().match(BAN_KEY_PREFIX + "*").count(100).build();
    try (Cursor<String> cursor = redisTemplate.scan(options)) {
      while (cursor.hasNext()) {
        String key = cursor.next();
        if (key == null || !key.startsWith(BAN_KEY_PREFIX)) {
          continue;
        }
        String ip = extractIp(key);
        String current = banKeysByIp.get(ip);
        if (current == null && banKeysByIp.size() >= limit) {
          break;
        }
        if (current == null || isProjectedBanKey(key)) {
          banKeysByIp.put(ip, key);
        }
      }
    }
    List<String> banKeys = new ArrayList<>(banKeysByIp.values());
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
                  String ip = extractIp(banKey);
                  operations.opsForValue().get(reasonKeyForBanKey(banKey, ip));
                }
                return null;
              }
            });

    List<BanInfo> result = new ArrayList<>(Math.min(banKeys.size(), limit));
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
      String ip = extractIp(banKeys.get(i));
      String reason = pipelined.get(base + 1) instanceof String value ? value : "";
      result.add(new BanInfo(ip, reason == null ? "" : reason, ttlSeconds));
      if (result.size() >= limit) {
        break;
      }
    }
    return result;
  }

  @Override
  public List<BanInfo> listUnversionedBans(int max) {
    int limit = Math.max(max, 0);
    if (limit == 0) {
      return List.of();
    }
    List<String> banKeys = new ArrayList<>();
    ScanOptions options =
        ScanOptions.scanOptions()
            .match(BAN_KEY_PREFIX + "[^{]*")
            .count(LEGACY_SCAN_CANDIDATE_LIMIT)
            .build();
    try (Cursor<String> cursor = redisTemplate.scan(options)) {
      while (cursor.hasNext() && banKeys.size() < limit) {
        String key = cursor.next();
        if (key != null && key.startsWith(BAN_KEY_PREFIX) && !isProjectedBanKey(key)) {
          banKeys.add(key);
        }
      }
    }
    return readBanInfos(banKeys, limit);
  }

  private List<BanInfo> readBanInfos(List<String> banKeys, int limit) {
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
                  String ip = extractIp(banKey);
                  operations.opsForValue().get(reasonKeyForBanKey(banKey, ip));
                }
                return null;
              }
            });
    List<BanInfo> result = new ArrayList<>(Math.min(banKeys.size(), limit));
    if (pipelined == null) {
      return result;
    }
    for (int i = 0; i < banKeys.size(); i++) {
      int base = i * 2;
      if (base + 1 >= pipelined.size()) {
        break;
      }
      Long ttlSeconds = pipelined.get(base) instanceof Long ttl ? ttl : null;
      if (ttlSeconds == null || ttlSeconds == -2L) {
        continue;
      }
      String ip = extractIp(banKeys.get(i));
      String reason = pipelined.get(base + 1) instanceof String value ? value : "";
      result.add(new BanInfo(ip, reason == null ? "" : reason, ttlSeconds));
      if (result.size() >= limit) {
        break;
      }
    }
    return result;
  }

  @Override
  public ConsumeResult tryConsume(String key, int limit, Duration window) {
    String redisKey = RATE_KEY_PREFIX + key;
    List<String> keys = Collections.singletonList(redisKey);
    List<?> raw =
        executeRedis(
            () ->
                rateLimitTemplate()
                    .execute(TRY_CONSUME_SCRIPT, keys, String.valueOf(window.getSeconds())));
    if (raw == null
        || raw.size() < 2
        || !(raw.get(0) instanceof Long count)
        || !(raw.get(1) instanceof Long pttlMs)) {
      throw new IllegalStateException("redis tryConsume script returned malformed result");
    }
    long retryAfterSeconds = pttlMs > 0 ? Math.max(1, (pttlMs + 999) / 1000) : window.getSeconds();
    if (count > limit) {
      return ConsumeResult.deny(retryAfterSeconds);
    }
    return ConsumeResult.allow(limit - count);
  }

  private static String banKey(String ip) {
    return BAN_KEY_PREFIX + hashTag(ip);
  }

  private StringRedisTemplate rateLimitTemplate() {
    int index =
        (int)
            Math.floorMod(nextRateLimitTemplateIndex.getAndIncrement(), rateLimitTemplates.size());
    return rateLimitTemplates.get(index);
  }

  private static byte[] serializeKey(String key) {
    return key.getBytes(StandardCharsets.UTF_8);
  }

  private static long toTtlSeconds(long ttlMillis) {
    return ttlMillis < 0 ? ttlMillis : TimeUnit.MILLISECONDS.toSeconds(ttlMillis);
  }

  private static AccessControlStore.UnavailableException unavailablePttl() {
    return new AccessControlStore.UnavailableException(
        new IllegalStateException("redis PTTL returned null"));
  }

  private static <T> T executeRedis(RedisOperation<T> operation) {
    try {
      return operation.execute();
    } catch (DataAccessException exception) {
      throw new AccessControlStore.UnavailableException(exception);
    }
  }

  private static String reasonKey(String ip) {
    return BAN_REASON_PREFIX + hashTag(ip);
  }

  private static String versionKey(String ip) {
    return BAN_VERSION_PREFIX + hashTag(ip);
  }

  private static String hashTag(String ip) {
    return "{" + ip + "}";
  }

  private static String extractIp(String banKey) {
    String suffix = banKey.substring(BAN_KEY_PREFIX.length());
    if (suffix.startsWith("{") && suffix.endsWith("}")) {
      return suffix.substring(1, suffix.length() - 1);
    }
    return suffix;
  }

  private static String legacyBanKey(String ip) {
    return BAN_KEY_PREFIX + ip;
  }

  private static String legacyReasonKey(String ip) {
    return BAN_REASON_PREFIX + ip;
  }

  private static boolean isProjectedBanKey(String key) {
    String suffix = key.substring(BAN_KEY_PREFIX.length());
    return suffix.startsWith("{") && suffix.endsWith("}");
  }

  private static String reasonKeyForBanKey(String banKey, String ip) {
    return isProjectedBanKey(banKey) ? reasonKey(ip) : legacyReasonKey(ip);
  }

  @FunctionalInterface
  private interface RedisOperation<T> {

    T execute();
  }
}
