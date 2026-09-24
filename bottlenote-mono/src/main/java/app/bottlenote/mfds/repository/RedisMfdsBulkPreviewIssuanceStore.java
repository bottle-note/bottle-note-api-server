package app.bottlenote.mfds.repository;

import app.bottlenote.mfds.domain.MfdsBulkPreviewIssuance;
import app.bottlenote.mfds.domain.MfdsBulkPreviewIssuanceStore;
import app.bottlenote.mfds.exception.MfdsException;
import app.bottlenote.mfds.exception.MfdsExceptionCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/** 기존 Redis에 미리보기 발급 기록을 TTL과 함께 둔다. 연결 실패는 재시도하지 않고 예외로 올린다. */
@Repository
public class RedisMfdsBulkPreviewIssuanceStore implements MfdsBulkPreviewIssuanceStore {

  static final String PREFIX = "mfds:bulk-preview:";

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  public RedisMfdsBulkPreviewIssuanceStore(
      StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
    this.redisTemplate = redisTemplate;
    this.objectMapper = objectMapper;
  }

  @Override
  public void save(MfdsBulkPreviewIssuance issuance, Duration ttl) {
    try {
      redisTemplate
          .opsForValue()
          .set(PREFIX + issuance.token(), objectMapper.writeValueAsString(issuance), ttl);
    } catch (RuntimeException | JsonProcessingException exception) {
      throw new MfdsException(MfdsExceptionCode.MFDS_BULK_PREVIEW_STORE_UNAVAILABLE);
    }
  }

  @Override
  public Optional<MfdsBulkPreviewIssuance> consume(String token) {
    try {
      String stored = redisTemplate.opsForValue().getAndDelete(PREFIX + token);
      if (stored == null) {
        return Optional.empty();
      }
      return Optional.of(objectMapper.readValue(stored, MfdsBulkPreviewIssuance.class));
    } catch (RuntimeException | JsonProcessingException exception) {
      throw new MfdsException(MfdsExceptionCode.MFDS_BULK_PREVIEW_STORE_UNAVAILABLE);
    }
  }
}
