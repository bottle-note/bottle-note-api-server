package app.bottlenote.global.security.accesscontrol;

import java.time.Duration;
import java.util.List;

/** IP ban + rate limit 상태 저장소 (운영: Redis) */
public interface AccessControlStore {

  boolean isBanned(String ip);

  void ban(String ip, Duration ttl, String reason);

  void unban(String ip);

  /** DB 감사 이벤트 순서를 이용해 Redis enforcement를 단조롭게 갱신한다. */
  default void projectBan(String ip, Duration ttl, String reason, long eventId) {
    ban(ip, ttl, reason);
  }

  /** 늦게 도착한 이전 밴이 해제를 덮어쓰지 않도록 버전을 보존한다. */
  default void projectUnban(String ip, long eventId) {
    unban(ip);
  }

  /** DB 관리 이전의 version 없는 legacy ban만 제거한다. */
  default void removeUnversionedBan(String ip) {
    unban(ip);
  }

  BanInfo getBan(String ip);

  /** 활성 ban 목록 (최대 max 개, 운영 inventory용). */
  List<BanInfo> listBans(int max);

  /**
   * fixed-window 카운터를 증가시킨다.
   *
   * @return remaining &gt;= 0 이면 허용, remaining &lt; 0 이면 거부 (retryAfterSeconds 유효)
   */
  ConsumeResult tryConsume(String key, int limit, Duration window);

  record BanInfo(String ip, String reason, long ttlSeconds) {}

  record ConsumeResult(long remaining, long retryAfterSeconds) {
    public boolean allowed() {
      return remaining >= 0;
    }

    public static ConsumeResult allow(long remaining) {
      return new ConsumeResult(remaining, 0);
    }

    public static ConsumeResult deny(long retryAfterSeconds) {
      return new ConsumeResult(-1, Math.max(retryAfterSeconds, 1));
    }
  }
}
