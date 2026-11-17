package app.bottlenote.curation.service;

import app.bottlenote.curation.domain.CurationFeedRegenerationLock;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// 스펙 변경 후 Read Model을 되살리는 절차를 소유한다. 잠금·순서·실패 정책이 여기 모인다.
@Service
@RequiredArgsConstructor
@Slf4j
public class CurationFeedPayloadRefreshService {

  private final CurationFeedPayloadRegenerationService regenerationService;
  private final CurationFeedRegenerationLock lock;

  // feed_payload는 파생 데이터고 NULL fallback이 있다. 어떤 실패도 호출자(기동 경로)로 새지 않는다.
  // 무효화와 재생성이 각자 커밋되어야 하므로 이 메서드 자체는 트랜잭션을 열지 않는다.
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void refresh(Collection<Long> specIds) {
    if (specIds == null || specIds.isEmpty()) {
      return;
    }
    boolean acquired = false;
    try {
      acquired = lock.tryAcquire();
      if (!acquired) {
        log.info("다른 인스턴스가 재생성 중이라 건너뜁니다: specIds={}", specIds);
        return;
      }
      // 무효화를 먼저 커밋한다. 뒤가 실패해도 낡은 값이 아니라 NULL이 남아 원본으로 fallback된다.
      regenerationService.invalidate(specIds);
      log.info(
          "feed_payload 재생성 완료: specIds={}, curations={}",
          specIds,
          regenerationService.regenerate(specIds));
    } catch (Exception e) {
      log.warn("feed_payload 재생성 실패. 조회는 원본 payload로 대체됩니다: specIds={}", specIds, e);
    } finally {
      releaseQuietly(acquired);
    }
  }

  private void releaseQuietly(boolean acquired) {
    if (!acquired) {
      return;
    }
    try {
      lock.release();
    } catch (Exception e) {
      log.warn("재생성 락 해제에 실패했습니다. TTL로 만료됩니다.", e);
    }
  }
}
