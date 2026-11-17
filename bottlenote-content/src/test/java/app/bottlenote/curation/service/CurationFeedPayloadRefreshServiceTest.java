package app.bottlenote.curation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import app.bottlenote.curation.domain.CurationFeedRegenerationLock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("CurationFeedPayloadRefreshService 단위 테스트")
class CurationFeedPayloadRefreshServiceTest {

  @Test
  @DisplayName("변경된 스펙이 없으면 락을 잡지 않는다")
  void refresh_whenNoSpecIds_doesNothing() {
    FakeLock lock = new FakeLock(true);
    RecordingRegeneration regeneration = new RecordingRegeneration();

    new CurationFeedPayloadRefreshService(regeneration, lock).refresh(List.of());
    new CurationFeedPayloadRefreshService(regeneration, lock).refresh(null);

    assertThat(lock.acquireCount).isZero();
    assertThat(regeneration.regenerated).isEmpty();
  }

  @Test
  @DisplayName("무효화를 먼저 커밋한 뒤 재생성하고 락을 해제한다")
  void refresh_invalidatesBeforeRegenerating() {
    FakeLock lock = new FakeLock(true);
    RecordingRegeneration regeneration = new RecordingRegeneration();

    new CurationFeedPayloadRefreshService(regeneration, lock).refresh(List.of(1L, 3L));

    assertThat(regeneration.order).containsExactly("invalidate", "regenerate");
    assertThat(regeneration.regenerated).containsExactly(List.of(1L, 3L));
    assertThat(lock.releaseCount).isEqualTo(1);
  }

  @Test
  @DisplayName("락을 얻지 못하면 재생성을 건너뛰고 해제도 하지 않는다")
  void refresh_whenLockNotAcquired_skips() {
    FakeLock lock = new FakeLock(false);
    RecordingRegeneration regeneration = new RecordingRegeneration();

    new CurationFeedPayloadRefreshService(regeneration, lock).refresh(List.of(1L));

    assertThat(regeneration.order).isEmpty();
    assertThat(lock.releaseCount).isZero();
  }

  @Test
  @DisplayName("재생성이 실패해도 호출자로 예외가 새지 않고 락은 해제된다")
  void refresh_whenRegenerationFails_swallowsAndReleases() {
    FakeLock lock = new FakeLock(true);
    RecordingRegeneration regeneration =
        new RecordingRegeneration() {
          @Override
          public int regenerate(Collection<Long> specIds) {
            throw new IllegalStateException("추출 실패");
          }
        };
    CurationFeedPayloadRefreshService service =
        new CurationFeedPayloadRefreshService(regeneration, lock);

    assertThatCode(() -> service.refresh(List.of(1L))).doesNotThrowAnyException();
    assertThat(lock.releaseCount).isEqualTo(1);
  }

  @Test
  @DisplayName("락 획득 중 저장소가 죽어도 예외가 새지 않는다")
  void refresh_whenAcquireThrows_swallows() {
    CurationFeedPayloadRefreshService service =
        new CurationFeedPayloadRefreshService(new RecordingRegeneration(), new ExplodingLock(true));

    assertThatCode(() -> service.refresh(List.of(1L))).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("락 해제 중 저장소가 죽어도 예외가 새지 않는다")
  void refresh_whenReleaseThrows_swallows() {
    CurationFeedPayloadRefreshService service =
        new CurationFeedPayloadRefreshService(
            new RecordingRegeneration(), new ExplodingLock(false));

    assertThatCode(() -> service.refresh(List.of(1L))).doesNotThrowAnyException();
  }

  private static class RecordingRegeneration extends CurationFeedPayloadRegenerationService {
    final List<String> order = new ArrayList<>();
    final List<Collection<Long>> regenerated = new ArrayList<>();

    RecordingRegeneration() {
      super(null, null, null);
    }

    @Override
    public int invalidate(Collection<Long> specIds) {
      order.add("invalidate");
      return specIds.size();
    }

    @Override
    public int regenerate(Collection<Long> specIds) {
      order.add("regenerate");
      regenerated.add(specIds);
      return specIds.size();
    }
  }

  private static class FakeLock implements CurationFeedRegenerationLock {
    private final boolean acquirable;
    int acquireCount;
    int releaseCount;

    FakeLock(boolean acquirable) {
      this.acquirable = acquirable;
    }

    @Override
    public boolean tryAcquire() {
      acquireCount++;
      return acquirable;
    }

    @Override
    public void release() {
      releaseCount++;
    }
  }

  // 공유 저장소 장애를 흉내낸다. 기동 경로에서 이 예외가 새면 애플리케이션이 뜨지 않는다.
  private record ExplodingLock(boolean onAcquire) implements CurationFeedRegenerationLock {
    @Override
    public boolean tryAcquire() {
      if (onAcquire) {
        throw new IllegalStateException("Redis 연결 실패");
      }
      return true;
    }

    @Override
    public void release() {
      throw new IllegalStateException("Redis 연결 실패");
    }
  }
}
