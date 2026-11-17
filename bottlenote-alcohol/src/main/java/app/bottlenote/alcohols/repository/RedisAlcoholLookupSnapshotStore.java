package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.AlcoholLookupSnapshotStore;
import app.bottlenote.alcohols.dto.response.AlcoholLookupSnapshotItem;
import app.bottlenote.global.redis.config.LettuceClientSupport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class RedisAlcoholLookupSnapshotStore implements AlcoholLookupSnapshotStore, DisposableBean {
  private static final String LOOKUP_SNAPSHOT_KEY = "alcohol:lookup:snapshot:v1";
  private static final String LOOKUP_VERSION_KEY = "alcohol:lookup:snapshot:version:v1";
  private static final TypeReference<List<AlcoholLookupSnapshotItem>> LOOKUP_ITEMS_TYPE =
      new TypeReference<>() {};

  private final RedisTemplate<String, Object> writeTemplate;
  private final RedisTemplate<String, Object> readTemplate;
  private final LettuceConnectionFactory readFactory;
  private final ObjectMapper objectMapper;

  public RedisAlcoholLookupSnapshotStore(
      RedisTemplate<String, Object> redisTemplate,
      RedisConnectionFactory redisConnectionFactory,
      ObjectMapper objectMapper,
      @Value("${alcohol.lookup.redis.read-timeout:2s}") Duration readTimeout) {
    this.writeTemplate = redisTemplate;
    this.objectMapper = objectMapper;
    this.readFactory = LettuceClientSupport.dedicatedFactory(redisConnectionFactory, readTimeout);
    try {
      LettuceClientSupport.start(this.readFactory);
    } catch (RuntimeException exception) {
      this.readFactory.destroy();
      throw exception;
    }
    this.readTemplate = cloneTemplate(redisTemplate, this.readFactory);
  }

  @Override
  public List<AlcoholLookupSnapshotItem> findAll() {
    Object value = readTemplate.opsForValue().get(LOOKUP_SNAPSHOT_KEY);
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
    Object value = readTemplate.opsForValue().get(LOOKUP_VERSION_KEY);
    return value == null ? Optional.empty() : Optional.of(value.toString());
  }

  @Override
  public void replaceAll(List<AlcoholLookupSnapshotItem> items) {
    final String snapshotJson;
    try {
      snapshotJson = objectMapper.writeValueAsString(items);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Alcohol lookup snapshot 직렬화에 실패했습니다.", e);
    }
    final String version = UUID.randomUUID().toString();
    List<Object> execResult =
        writeTemplate.execute(
            new SessionCallback<List<Object>>() {
              @Override
              @SuppressWarnings("unchecked")
              public List<Object> execute(RedisOperations operations) {
                operations.multi();
                operations.opsForValue().set(LOOKUP_SNAPSHOT_KEY, snapshotJson);
                operations.opsForValue().set(LOOKUP_VERSION_KEY, version);
                return operations.exec();
              }
            });
    if (execResult == null) {
      throw new IllegalStateException("Alcohol lookup snapshot 원자 갱신이 중단되었습니다.");
    }
  }

  @Override
  public void destroy() {
    readFactory.destroy();
  }

  private static RedisTemplate<String, Object> cloneTemplate(
      RedisTemplate<String, Object> source, RedisConnectionFactory factory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(factory);
    template.setKeySerializer(source.getKeySerializer());
    template.setValueSerializer(source.getValueSerializer());
    template.setHashKeySerializer(source.getHashKeySerializer());
    template.setHashValueSerializer(source.getHashValueSerializer());
    template.afterPropertiesSet();
    return template;
  }
}
