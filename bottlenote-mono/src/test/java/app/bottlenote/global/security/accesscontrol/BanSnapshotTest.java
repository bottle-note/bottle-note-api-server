package app.bottlenote.global.security.accesscontrol;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.global.security.accesscontrol.AccessControlStore.BanInfo;
import app.bottlenote.global.security.accesscontrol.fixture.InMemoryAccessControlStore;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("BanSnapshot 단위 테스트")
class BanSnapshotTest {

  @Test
  @DisplayName("snapshot은 입력 Map과 분리된 immutable 상태를 보관한다")
  void snapshot_whenCreated_copiesEntriesImmutably() {
    Instant now = Instant.parse("2026-08-09T00:00:00Z");
    Map<String, BanSnapshot.BanEntry> source = new LinkedHashMap<>();
    source.put("203.0.113.10", new BanSnapshot.BanEntry(now.plusSeconds(30), "abuse"));

    BanSnapshot snapshot = new BanSnapshot(source, now);
    source.clear();

    assertThat(snapshot.entries()).containsKey("203.0.113.10");
    assertThat(snapshot.entries()).isUnmodifiable();
  }

  @Test
  @DisplayName("snapshot은 hit, missing, expiry를 판정한다")
  void lookup_whenHitMissingAndExpired_returnsExpectedEntry() {
    Instant now = Instant.parse("2026-08-09T00:00:00Z");
    BanSnapshot snapshot =
        new BanSnapshot(
            Map.of(
                "203.0.113.10", new BanSnapshot.BanEntry(now.plusSeconds(1), "active"),
                "203.0.113.11", new BanSnapshot.BanEntry(now, "expired")),
            now);

    assertThat(snapshot.lookup("203.0.113.10", now)).isPresent();
    assertThat(snapshot.lookup("203.0.113.99", now)).isEmpty();
    assertThat(snapshot.lookup("203.0.113.11", now)).isEmpty();
  }

  @Test
  @DisplayName("empty snapshot은 startup에서 즉시 stale이다")
  void empty_whenStartup_isStale() {
    assertThat(
            BanSnapshot.empty()
                .isStale(Instant.parse("2026-08-09T00:00:00Z"), Duration.ofMinutes(3)))
        .isTrue();
  }

  @Test
  @DisplayName("마지막 성공 후 3분을 초과하면 stale이다")
  void isStale_whenThresholdExceeded_returnsTrue() {
    Instant refreshedAt = Instant.parse("2026-08-09T00:00:00Z");
    BanSnapshot snapshot = new BanSnapshot(Map.of(), refreshedAt);

    assertThat(snapshot.isStale(refreshedAt.plus(Duration.ofMinutes(3)), Duration.ofMinutes(3)))
        .isFalse();
    assertThat(
            snapshot.isStale(
                refreshedAt.plus(Duration.ofMinutes(3).plusSeconds(1)), Duration.ofMinutes(3)))
        .isTrue();
  }

  @Test
  @DisplayName("holder는 새 snapshot을 원자적으로 교체하고 만료가 늦은 항목을 보존한다")
  void replace_whenEntriesExceedMaximum_atomicallyKeepsLatestExpiry() {
    Instant now = Instant.parse("2026-08-09T00:00:00Z");
    BanSnapshotHolder holder = new BanSnapshotHolder(2);
    BanSnapshot snapshot =
        new BanSnapshot(
            Map.of(
                "203.0.113.10", new BanSnapshot.BanEntry(now.plusSeconds(10), "short"),
                "203.0.113.11", new BanSnapshot.BanEntry(now.plusSeconds(20), "middle"),
                "203.0.113.12", new BanSnapshot.BanEntry(now.plusSeconds(30), "long")),
            now);

    holder.replace(snapshot);

    assertThat(holder.get().entries()).containsOnlyKeys("203.0.113.11", "203.0.113.12").hasSize(2);
  }

  @Test
  @DisplayName("동시 읽기는 교체 중에도 완전한 snapshot만 관찰한다")
  void get_whenConcurrentReplace_readsOnlyCompleteSnapshots() throws Exception {
    Instant now = Instant.parse("2026-08-09T00:00:00Z");
    BanSnapshot first =
        new BanSnapshot(
            Map.of("203.0.113.10", new BanSnapshot.BanEntry(now.plusSeconds(30), "first")), now);
    BanSnapshot second =
        new BanSnapshot(
            Map.of("203.0.113.11", new BanSnapshot.BanEntry(now.plusSeconds(30), "second")), now);
    BanSnapshotHolder holder = new BanSnapshotHolder(10);
    holder.replace(first);
    CountDownLatch started = new CountDownLatch(1);

    try (ExecutorService executor = Executors.newFixedThreadPool(8)) {
      Future<?> writer =
          executor.submit(
              () -> {
                started.await();
                for (int i = 0; i < 10_000; i++) {
                  holder.replace((i & 1) == 0 ? second : first);
                }
                return null;
              });
      List<Future<Boolean>> readers =
          IntStream.range(0, 100)
              .mapToObj(
                  ignored ->
                      executor.submit(
                          (Callable<Boolean>)
                              () -> {
                                started.await();
                                for (int i = 0; i < 1_000; i++) {
                                  Map<String, BanSnapshot.BanEntry> entries =
                                      holder.get().entries();
                                  if (!entries.equals(first.entries())
                                      && !entries.equals(second.entries())) {
                                    return false;
                                  }
                                }
                                return true;
                              }))
              .toList();

      started.countDown();
      writer.get();
      for (Future<Boolean> reader : readers) {
        assertThat(reader.get()).isTrue();
      }
    }
  }

  @Test
  @DisplayName("refresh는 InMemory ban과 무기한 TTL을 snapshot에 반영한다")
  void refresh_whenStoreSucceeds_updatesSnapshotAndPreservesIndefiniteTtl() {
    Instant now = Instant.parse("2026-08-09T00:00:00Z");
    InMemoryAccessControlStore store = new InMemoryAccessControlStore();
    store.ban("203.0.113.10", Duration.ofMinutes(5), "finite");
    BanSnapshotHolder holder = new BanSnapshotHolder(10);
    BanSnapshotRefresher refresher =
        new BanSnapshotRefresher(
            new IndefiniteBanStore(store), holder, 10, Clock.fixed(now, java.time.ZoneOffset.UTC));

    refresher.refresh();

    assertThat(holder.get().lookup("203.0.113.10", now)).isPresent();
    assertThat(holder.get().lookup("203.0.113.11", now)).isPresent();
    assertThat(holder.get().lookup("203.0.113.12", now)).isPresent();
    assertThat(holder.get().lookup("203.0.113.12", now.plusSeconds(1))).isEmpty();
    assertThat(holder.get().entries().get("203.0.113.11").expiresAt()).isEqualTo(Instant.MAX);
  }

  @Test
  @DisplayName("refresh 실패는 정확히 기존 snapshot을 유지한다")
  void refresh_whenStoreFails_retainsExactPreviousSnapshot() {
    Instant now = Instant.parse("2026-08-09T00:00:00Z");
    BanSnapshot previous =
        new BanSnapshot(
            Map.of("203.0.113.10", new BanSnapshot.BanEntry(now.plusSeconds(30), "existing")), now);
    BanSnapshotHolder holder = new BanSnapshotHolder(10);
    holder.replace(previous);
    BanSnapshotRefresher refresher =
        new BanSnapshotRefresher(
            new FailingAccessControlStore(),
            holder,
            10,
            Clock.fixed(now, java.time.ZoneOffset.UTC));

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
      List<BanInfo> finiteBans = delegate.listBans(max);
      return List.of(
          finiteBans.getFirst(),
          new BanInfo("203.0.113.11", "indefinite", -1L),
          new BanInfo("203.0.113.12", "last-second", 0L));
    }
  }

  private static final class FailingAccessControlStore extends InMemoryAccessControlStore {
    @Override
    public List<BanInfo> listBans(int max) {
      throw new AccessControlStoreUnavailableException(new IllegalStateException("redis down"));
    }
  }
}
