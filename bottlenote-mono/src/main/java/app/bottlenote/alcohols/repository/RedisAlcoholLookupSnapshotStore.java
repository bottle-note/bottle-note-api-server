package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.AlcoholLookupSnapshotStore;
import app.bottlenote.alcohols.dto.response.AlcoholLookupSnapshotItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisAlcoholLookupSnapshotStore implements AlcoholLookupSnapshotStore {
  private static final String LOOKUP_SNAPSHOT_KEY = "alcohol:lookup:snapshot:v1";
  private static final String LOOKUP_VERSION_KEY = "alcohol:lookup:snapshot:version:v1";
  private static final TypeReference<List<AlcoholLookupSnapshotItem>> LOOKUP_ITEMS_TYPE =
      new TypeReference<>() {};

  private final RedisTemplate<String, Object> redisTemplate;
  private final ObjectMapper objectMapper;

  @Override
  public List<AlcoholLookupSnapshotItem> findAll() {
    Object value = redisTemplate.opsForValue().get(LOOKUP_SNAPSHOT_KEY);
    if (value == null) {
      return List.of();
    }

    try {
      return objectMapper.readValue(value.toString(), LOOKUP_ITEMS_TYPE);
    } catch (JsonProcessingException e) {
      log.warn("Alcohol lookup snapshot 역직렬화 실패. Redis snapshot을 비어 있는 것으로 처리합니다.", e);
      return List.of();
    }
  }

  @Override
  public Optional<String> findVersion() {
    Object value = redisTemplate.opsForValue().get(LOOKUP_VERSION_KEY);
    return value == null ? Optional.empty() : Optional.of(value.toString());
  }

  @Override
  public void replaceAll(List<AlcoholLookupSnapshotItem> items) {
    try {
      redisTemplate.opsForValue().set(LOOKUP_SNAPSHOT_KEY, objectMapper.writeValueAsString(items));
      redisTemplate.opsForValue().set(LOOKUP_VERSION_KEY, UUID.randomUUID().toString());
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Alcohol lookup snapshot 직렬화에 실패했습니다.", e);
    }
  }
}
