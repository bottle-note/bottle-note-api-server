package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.constant.AlcoholCategoryGroup.SINGLE_MALT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.alcohols.dto.response.AlcoholLookupItem;
import app.bottlenote.alcohols.dto.response.AlcoholLookupSnapshotItem;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.alcohols.exception.AlcoholExceptionCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("LookupDatabaseFallbackGuard 단위 테스트")
class LookupDatabaseFallbackGuardTest {

  @Test
  @DisplayName("TTL 안에서는 직전 DB 결과를 재사용해 추가 조회를 막는다")
  void load_whenResultTtlFresh_reusesLastSuccess() {
    MutableClock clock = new MutableClock(Instant.parse("2026-08-23T00:00:00Z"));
    LookupDatabaseFallbackGuard guard =
        new LookupDatabaseFallbackGuard(clock, Duration.ofSeconds(30), 3, Duration.ofSeconds(30));
    AtomicInteger calls = new AtomicInteger();
    List<AlcoholLookupSnapshotItem> items = List.of(snapshotItem(1L));

    guard.load(
        () -> {
          calls.incrementAndGet();
          return items;
        });
    List<AlcoholLookupSnapshotItem> second =
        guard.load(
            () -> {
              calls.incrementAndGet();
              return List.of(snapshotItem(2L));
            });

    assertThat(calls.get()).isEqualTo(1);
    assertThat(second).extracting(AlcoholLookupSnapshotItem::alcoholId).containsExactly(1L);
  }

  @Test
  @DisplayName("DB 조회가 오래 걸려도 성공 결과 TTL은 완료 시점부터 계산한다")
  void load_whenLoaderIsSlow_startsResultTtlAfterCompletion() {
    MutableClock clock = new MutableClock(Instant.parse("2026-08-23T00:00:00Z"));
    LookupDatabaseFallbackGuard guard =
        new LookupDatabaseFallbackGuard(clock, Duration.ofSeconds(30), 3, Duration.ofSeconds(30));
    AtomicInteger calls = new AtomicInteger();

    guard.load(
        () -> {
          calls.incrementAndGet();
          clock.advance(Duration.ofSeconds(31));
          return List.of(snapshotItem(1L));
        });
    List<AlcoholLookupSnapshotItem> second =
        guard.load(
            () -> {
              calls.incrementAndGet();
              return List.of(snapshotItem(2L));
            });

    assertThat(calls.get()).isEqualTo(1);
    assertThat(second).extracting(AlcoholLookupSnapshotItem::alcoholId).containsExactly(1L);
  }

  @Test
  @DisplayName("연속 실패가 임계치를 넘으면 circuit을 열어 추가 DB 조회를 생략한다")
  void load_whenFailuresReachThreshold_opensCircuit() {
    LookupDatabaseFallbackGuard guard =
        new LookupDatabaseFallbackGuard(
            Clock.systemUTC(), Duration.ofSeconds(30), 3, Duration.ofSeconds(30));
    AtomicInteger calls = new AtomicInteger();

    for (int i = 0; i < 3; i++) {
      assertThatThrownBy(
              () ->
                  guard.load(
                      () -> {
                        calls.incrementAndGet();
                        throw new IllegalStateException("db down");
                      }))
          .isInstanceOf(IllegalStateException.class);
    }

    // 캐시된 성공 결과가 없는 완전 실패는 빈 목록으로 감추지 않고 503으로 노출한다.
    assertThatThrownBy(
            () ->
                guard.load(
                    () -> {
                      calls.incrementAndGet();
                      throw new IllegalStateException("should not run");
                    }))
        .isInstanceOf(AlcoholException.class)
        .hasMessage(AlcoholExceptionCode.ALCOHOL_LOOKUP_UNAVAILABLE.getMessage());

    assertThat(calls.get()).isEqualTo(3);
  }

  @Test
  @DisplayName("DB 조회가 오래 걸려도 circuit 차단 시간은 실패 시점부터 계산한다")
  void load_whenFailureIsSlow_startsCircuitOpenDurationAfterFailure() {
    MutableClock clock = new MutableClock(Instant.parse("2026-08-23T00:00:00Z"));
    LookupDatabaseFallbackGuard guard =
        new LookupDatabaseFallbackGuard(clock, Duration.ZERO, 1, Duration.ofSeconds(30));
    AtomicInteger calls = new AtomicInteger();

    assertThatThrownBy(
            () ->
                guard.load(
                    () -> {
                      calls.incrementAndGet();
                      clock.advance(Duration.ofSeconds(31));
                      throw new IllegalStateException("db down");
                    }))
        .isInstanceOf(IllegalStateException.class);

    assertThatThrownBy(
            () ->
                guard.load(
                    () -> {
                      calls.incrementAndGet();
                      throw new IllegalStateException("should not run");
                    }))
        .isInstanceOf(AlcoholException.class);
    assertThat(calls.get()).isEqualTo(1);
  }

  @Test
  @DisplayName("circuit이 열려도 직전 성공 결과가 있으면 그 결과로 응답한다")
  void load_whenCircuitOpenWithCachedResult_servesCache() {
    LookupDatabaseFallbackGuard guard =
        new LookupDatabaseFallbackGuard(
            Clock.systemUTC(), Duration.ZERO, 1, Duration.ofSeconds(30));

    assertThat(guard.load(() -> List.of(snapshotItem(1L))))
        .extracting(AlcoholLookupSnapshotItem::alcoholId)
        .containsExactly(1L);

    // 실패해도 직전 성공 결과가 있으면 그것으로 응답하고, 이때 circuit이 열린다.
    assertThat(
            guard.load(
                () -> {
                  throw new IllegalStateException("db down");
                }))
        .extracting(AlcoholLookupSnapshotItem::alcoholId)
        .containsExactly(1L);

    AtomicInteger blockedCalls = new AtomicInteger();
    assertThat(
            guard.load(
                () -> {
                  blockedCalls.incrementAndGet();
                  throw new IllegalStateException("should not run");
                }))
        .extracting(AlcoholLookupSnapshotItem::alcoholId)
        .containsExactly(1L);
    assertThat(blockedCalls.get()).isZero();
  }

  @Test
  @DisplayName("동시에 들어온 fallback은 한 번의 DB 조회로 합친다")
  void load_whenConcurrent_usesSingleFlight() throws Exception {
    // TTL을 두면 in-flight join을 놓친 늦은 도착도 캐시를 타므로, 스레드 순서와
    // 무관하게 DB 조회는 정확히 1회로 확정된다.
    LookupDatabaseFallbackGuard guard =
        new LookupDatabaseFallbackGuard(
            Clock.systemUTC(), Duration.ofSeconds(30), 3, Duration.ofSeconds(30));
    AtomicInteger calls = new AtomicInteger();
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(8);

    try {
      // 선행 호출이 in-flight로 확정된 뒤에 나머지를 제출해야 결정적으로 검증된다.
      // 8개를 한꺼번에 제출하면 느린 러너에서 일부가 첫 로더 종료 후 도착해 두 번째
      // 조회가 발생한다.
      List<Future<List<AlcoholLookupSnapshotItem>>> futures = new java.util.ArrayList<>();
      futures.add(
          pool.submit(
              () ->
                  guard.load(
                      () -> {
                        calls.incrementAndGet();
                        entered.countDown();
                        await(release);
                        return List.of(snapshotItem(1L));
                      })));

      assertThat(entered.await(2, TimeUnit.SECONDS)).isTrue();

      for (int i = 0; i < 7; i++) {
        futures.add(
            pool.submit(
                () ->
                    guard.load(
                        () -> {
                          calls.incrementAndGet();
                          return List.of(snapshotItem(2L));
                        })));
      }

      release.countDown();
      for (Future<List<AlcoholLookupSnapshotItem>> future : futures) {
        assertThat(future.get(2, TimeUnit.SECONDS))
            .extracting(AlcoholLookupSnapshotItem::alcoholId)
            .containsExactly(1L);
      }
      assertThat(calls.get()).isEqualTo(1);
    } finally {
      pool.shutdownNow();
    }
  }

  private static void await(CountDownLatch latch) {
    try {
      if (!latch.await(2, TimeUnit.SECONDS)) {
        throw new IllegalStateException("release timeout");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  private static AlcoholLookupSnapshotItem snapshotItem(Long alcoholId) {
    return AlcoholLookupSnapshotItem.from(
        new AlcoholLookupItem(
            alcoholId,
            "맥캘란",
            "Macallan",
            "싱글몰트",
            "Single Malt",
            SINGLE_MALT,
            1L,
            "스페이사이드",
            "Speyside",
            10L,
            "맥캘란",
            "Macallan",
            "https://example.com/alcohol.png"));
  }

  private static final class MutableClock extends Clock {
    private Instant instant;

    private MutableClock(Instant instant) {
      this.instant = instant;
    }

    @Override
    public ZoneOffset getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(java.time.ZoneId zone) {
      return Clock.fixed(instant, zone);
    }

    @Override
    public Instant instant() {
      return instant;
    }

    private void advance(Duration duration) {
      instant = instant.plus(duration);
    }
  }
}
