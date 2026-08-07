package app.bottlenote.global.security.accesscontrol;

import java.time.Duration;

/** IP ban + rate limit 상태 저장소 (Redis / InMemory) */
public interface AccessControlStore {

  boolean isBanned(String ip);

  void ban(String ip, Duration ttl, String reason);

  void unban(String ip);

  BanInfo getBan(String ip);

  /**
   * fixed-window 카운터를 증가시키고 허용 여부를 반환한다.
   *
   * @return 허용이면 remaining(>=0), 초과면 -1
   */
  long tryConsume(String key, int limit, Duration window);

  record BanInfo(String ip, String reason, long ttlSeconds) {}
}
