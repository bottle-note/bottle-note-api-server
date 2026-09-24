package app.bottlenote.mfds.fixture;

import app.bottlenote.mfds.domain.MfdsBulkPreviewIssuance;
import app.bottlenote.mfds.domain.MfdsBulkPreviewIssuanceStore;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** 단위 테스트용 발급 저장소. TTL 경과 판단은 서비스가 expiresAt으로 한다. */
public class InMemoryMfdsBulkPreviewIssuanceStore implements MfdsBulkPreviewIssuanceStore {

  private final ConcurrentHashMap<String, MfdsBulkPreviewIssuance> values = new ConcurrentHashMap<>();

  @Override
  public void save(MfdsBulkPreviewIssuance issuance, Duration ttl) {
    values.put(issuance.token(), issuance);
  }

  @Override
  public Optional<MfdsBulkPreviewIssuance> consume(String token) {
    return Optional.ofNullable(values.remove(token));
  }
}
