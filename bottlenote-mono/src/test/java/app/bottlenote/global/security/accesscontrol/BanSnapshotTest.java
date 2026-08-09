package app.bottlenote.global.security.accesscontrol;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.global.security.accesscontrol.AccessControlStore.BanInfo;
import app.bottlenote.global.security.accesscontrol.AccessControlStore.UnavailableException;
import app.bottlenote.global.security.accesscontrol.BanSnapshotHolder.Entry;
import app.bottlenote.global.security.accesscontrol.BanSnapshotHolder.Snapshot;
import app.bottlenote.global.security.accesscontrol.fixture.InMemoryAccessControlStore;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("BanSnapshotHolder 단위 테스트")
class BanSnapshotTest {

  @Test
  @DisplayName("snapshot은 hit, missing, expiry를 판정한다")
  void snapshot_whenHitMissingAndExpired_returnsExpectedEntry() {
    Instant now = Instant.parse("2026-08-09T00:00:00Z");
    Snapshot snapshot =
        new Snapshot(
            Map.of(
                "203.0.113.10", new Entry(now.plusSeconds(1)),
                "203.0.113.11", new Entry(now)),
            now);

    assertThat(snapshot.lookup("203.0.113.10", now)).isPresent();
    assertThat(snapshot.lookup("203.0.113.99", now)).isEmpty();
    assertThat(snapshot.lookup("203.0.113.11", now)).isEmpty();
  }

  @Test
  @DisplayName("snapshot은 startup에서 stale이고 임계값 초과 시 stale이다")
  void snapshot_whenStartupOrThresholdExceeded_isStale() {
    Instant now = Instant.parse("2026-08-09T00:00:00Z");

    assertThat(Snapshot.empty().isStale(now, Duration.ofMinutes(3))).isTrue();
    assertThat(
            new Snapshot(Map.of(), now)
                .isStale(now.plus(Duration.ofMinutes(3)), Duration.ofMinutes(3)))
        .isFalse();
    assertThat(
            new Snapshot(Map.of(), now)
                .isStale(now.plus(Duration.ofMinutes(3).plusSeconds(1)), Duration.ofMinutes(3)))
        .isTrue();
  }

  @Test
  @DisplayName("holder는 상한을 넘으면 만료가 늦은 항목만 보존한다")
  void replace_whenEntriesExceedMaximum_keepsLatestExpiry() {
    Instant now = Instant.parse("2026-08-09T00:00:00Z");
    BanSnapshotHolder holder = new BanSnapshotHolder(2);

    holder.replace(
        new Snapshot(
            Map.of(
                "203.0.113.10", new Entry(now.plusSeconds(10)),
                "203.0.113.11", new Entry(now.plusSeconds(20)),
                "203.0.113.12", new Entry(now.plusSeconds(30))),
            now));

    assertThat(holder.get().entries()).containsOnlyKeys("203.0.113.11", "203.0.113.12").hasSize(2);
  }

  @Test
  @DisplayName("refresh는 유효한 ban과 무기한 TTL을 snapshot에 반영한다")
  void refresh_whenStoreSucceeds_updatesSnapshot() {
    Instant now = Instant.parse("2026-08-09T00:00:00Z");
    InMemoryAccessControlStore store = new InMemoryAccessControlStore();
    store.ban("203.0.113.10", Duration.ofMinutes(5), "finite");
    BanSnapshotHolder holder = new BanSnapshotHolder(10);
    BanSnapshotRefresher refresher =
        new BanSnapshotRefresher(
            new IndefiniteBanStore(store), holder, Clock.fixed(now, ZoneOffset.UTC));

    refresher.refresh();

    assertThat(holder.get().lookup("203.0.113.10", now)).isPresent();
    assertThat(holder.get().lookup("203.0.113.11", now)).isPresent();
    assertThat(holder.get().entries().get("203.0.113.11").expiresAt()).isEqualTo(Instant.MAX);
  }

  @Test
  @DisplayName("refresh 실패는 기존 snapshot을 유지한다")
  void refresh_whenStoreFails_retainsPreviousSnapshot() {
    Instant now = Instant.parse("2026-08-09T00:00:00Z");
    Snapshot previous = new Snapshot(Map.of("203.0.113.10", new Entry(now.plusSeconds(30))), now);
    BanSnapshotHolder holder = new BanSnapshotHolder(10);
    holder.replace(previous);
    BanSnapshotRefresher refresher =
        new BanSnapshotRefresher(
            new FailingAccessControlStore(), holder, Clock.fixed(now, ZoneOffset.UTC));

    refresher.refresh();

    assertThat(holder.get()).isSameAs(previous);
  }

  private static final class IndefiniteBanStore extends InMemoryAccessControlStore {
    private final InMemoryAccessControlStore delegate;

    private IndefiniteBanStore(InMemoryAccessControlStore delegate) {
      this.delegate = delegate;
    }

    @Override
    public List<BanInfo> listBans(int max) {
      return List.of(
          delegate.listBans(max).getFirst(), new BanInfo("203.0.113.11", "indefinite", -1L));
    }
  }

  private static final class FailingAccessControlStore extends InMemoryAccessControlStore {
    @Override
    public List<BanInfo> listBans(int max) {
      throw new UnavailableException(new IllegalStateException("redis down"));
    }
  }
}
