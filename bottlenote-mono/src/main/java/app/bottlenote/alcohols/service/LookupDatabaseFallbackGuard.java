package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.dto.response.AlcoholLookupSnapshotItem;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.alcohols.exception.AlcoholExceptionCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

/** 요청 경로 DB fallback을 single-flight와 circuit breaker로 보호한다. */
@Slf4j
final class LookupDatabaseFallbackGuard {

  private final Clock clock;
  private final Duration resultTtl;
  private final int failureThreshold;
  private final Duration openDuration;
  private final AtomicReference<CachedResult> lastSuccess = new AtomicReference<>();
  private final AtomicInteger consecutiveFailures = new AtomicInteger();
  private final AtomicLong openUntilMillis = new AtomicLong();
  private final AtomicBoolean halfOpenProbe = new AtomicBoolean();
  private final AtomicReference<CompletableFuture<List<AlcoholLookupSnapshotItem>>> inFlight =
      new AtomicReference<>();

  LookupDatabaseFallbackGuard(
      Clock clock, Duration resultTtl, int failureThreshold, Duration openDuration) {
    this.clock = clock;
    this.resultTtl = resultTtl;
    this.failureThreshold = Math.max(failureThreshold, 1);
    this.openDuration = openDuration;
  }

  List<AlcoholLookupSnapshotItem> load(Supplier<List<AlcoholLookupSnapshotItem>> loader) {
    Instant now = clock.instant();
    CachedResult cached = lastSuccess.get();
    if (isFresh(cached, now)) {
      return cached.items();
    }

    CompletableFuture<List<AlcoholLookupSnapshotItem>> existing = inFlight.get();
    if (existing != null) {
      return join(existing);
    }

    if (!allowLoad(now)) {
      log.warn("Alcohol lookup DB fallback circuit가 열려 추가 조회를 생략합니다.");
      if (cached == null) {
        // 캐시도 없는 완전 실패를 빈 목록으로 감추면 장애가 정상 응답으로 보인다.
        throw new AlcoholException(AlcoholExceptionCode.ALCOHOL_LOOKUP_UNAVAILABLE);
      }
      return cached.items();
    }

    return singleFlight(() -> invoke(loader, now));
  }

  private List<AlcoholLookupSnapshotItem> invoke(
      Supplier<List<AlcoholLookupSnapshotItem>> loader, Instant now) {
    try {
      List<AlcoholLookupSnapshotItem> items = List.copyOf(loader.get());
      lastSuccess.set(new CachedResult(items, now));
      consecutiveFailures.set(0);
      openUntilMillis.set(0);
      return items;
    } catch (RuntimeException exception) {
      int failures = consecutiveFailures.incrementAndGet();
      if (failures >= failureThreshold) {
        openUntilMillis.set(now.toEpochMilli() + openDuration.toMillis());
        log.warn(
            "Alcohol lookup DB fallback circuit를 엽니다. failures={}, openDuration={}",
            failures,
            openDuration);
      }
      CachedResult cached = lastSuccess.get();
      if (cached != null) {
        return cached.items();
      }
      throw exception;
    } finally {
      // Error로 빠져나가도 probe 플래그가 남으면 circuit이 영구히 열린 채로 고정된다.
      halfOpenProbe.set(false);
    }
  }

  private boolean allowLoad(Instant now) {
    long openUntil = openUntilMillis.get();
    if (openUntil == 0L) {
      return true;
    }
    if (now.toEpochMilli() < openUntil) {
      return false;
    }
    return halfOpenProbe.compareAndSet(false, true);
  }

  private boolean isFresh(CachedResult cached, Instant now) {
    if (cached == null || resultTtl.isZero() || resultTtl.isNegative()) {
      return false;
    }
    return cached.storedAt().plus(resultTtl).isAfter(now);
  }

  private List<AlcoholLookupSnapshotItem> singleFlight(
      Supplier<List<AlcoholLookupSnapshotItem>> loader) {
    while (true) {
      CompletableFuture<List<AlcoholLookupSnapshotItem>> existing = inFlight.get();
      if (existing != null) {
        return join(existing);
      }
      CompletableFuture<List<AlcoholLookupSnapshotItem>> created = new CompletableFuture<>();
      if (!inFlight.compareAndSet(null, created)) {
        continue;
      }
      try {
        List<AlcoholLookupSnapshotItem> items = loader.get();
        created.complete(items);
        return items;
      } catch (Throwable throwable) {
        // Error도 반드시 전달해야 join() 대기자가 영원히 묶이지 않는다.
        created.completeExceptionally(throwable);
        throw throwable;
      } finally {
        inFlight.compareAndSet(created, null);
      }
    }
  }

  private static List<AlcoholLookupSnapshotItem> join(
      CompletableFuture<List<AlcoholLookupSnapshotItem>> future) {
    try {
      return future.join();
    } catch (CompletionException exception) {
      Throwable cause = exception.getCause();
      if (cause instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      if (cause instanceof Error error) {
        throw error;
      }
      throw exception;
    }
  }

  private record CachedResult(List<AlcoholLookupSnapshotItem> items, Instant storedAt) {}
}
