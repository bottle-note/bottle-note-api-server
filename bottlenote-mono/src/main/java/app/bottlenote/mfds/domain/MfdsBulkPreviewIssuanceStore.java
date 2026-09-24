package app.bottlenote.mfds.domain;

import java.time.Duration;
import java.util.Optional;

/**
 * 미리보기 발급 기록. 구현은 기존 Redis를 쓰고, 테스트는 인메모리를 쓴다.
 *
 * <p>Redis 저장이나 조회가 실패하면 재시도하지 않고 호출자가 실패로 끝낸다. 키 없음은 미발급이다. DB 쓰기와 Redis 삭제는 한 트랜잭션이 아니므로, 토큰을 소비한
 * 뒤 DB가 롤백되면 사용자는 미리보기를 다시 받는다.
 */
public interface MfdsBulkPreviewIssuanceStore {

  void save(MfdsBulkPreviewIssuance issuance, Duration ttl);

  /** 발급 기록을 한 번만 꺼낸다. 없으면 empty다. */
  Optional<MfdsBulkPreviewIssuance> consume(String token);
}
