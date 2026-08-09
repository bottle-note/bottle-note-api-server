package app.bottlenote.global.security.accesscontrol;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;

/** Redis ban 목록을 주기적으로 로컬 snapshot으로 교체한다. */
@Slf4j
@RequiredArgsConstructor
public class BanSnapshotRefresher {

  private final AccessControlStore store;
  private final BanSnapshotHolder holder;
  private final int maxEntries;
  private final Clock clock;

  @Scheduled(fixedDelayString = "${bottlenote.access-control.snapshot.refresh-interval-ms:30000}")
  public void refresh() {
    try {
      List<AccessControlStore.BanInfo> bans = store.listBans(maxEntries);
      Instant now = clock.instant();
      Map<String, BanSnapshot.BanEntry> entries = new LinkedHashMap<>();
      for (AccessControlStore.BanInfo ban : bans) {
        Instant expiresAt =
            ban.ttlSeconds() < 0 ? Instant.MAX : now.plusSeconds(ban.ttlSeconds()).plusSeconds(1);
        entries.put(ban.ip(), new BanSnapshot.BanEntry(expiresAt, ban.reason()));
      }
      holder.replace(new BanSnapshot(entries, now));
      log.debug("ban snapshot 갱신 완료: {}건", entries.size());
    } catch (AccessControlStoreUnavailableException | DataAccessException exception) {
      log.warn(
          "ban snapshot 갱신 실패 — 기존 snapshot 유지 (refreshedAt={})",
          holder.get().refreshedAt(),
          exception);
    }
  }
}
