package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.AlcoholLookupSnapshotStore;
import app.bottlenote.alcohols.dto.request.AlcoholLookupRequest;
import app.bottlenote.alcohols.dto.response.AlcoholLookupSnapshotItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisAlcoholLookupSnapshotStore implements AlcoholLookupSnapshotStore {
  private static final String LOOKUP_SNAPSHOT_KEY = "alcohol:lookup:snapshot:v1";
  private static final String LOOKUP_VERSION_KEY = "alcohol:lookup:snapshot:version:v1";
  private static final String LOOKUP_ITEM_HASH_KEY = "alcohol:lookup:snapshot:item:v1";
  private static final String INDEX_KEY_PREFIX = "alcohol:lookup:{index:v1}:";
  private static final String INDEX_KEYS_KEY = INDEX_KEY_PREFIX + "keys";
  private static final TypeReference<List<AlcoholLookupSnapshotItem>> LOOKUP_ITEMS_TYPE =
      new TypeReference<>() {};

  private final RedisTemplate<String, Object> redisTemplate;
  private final ObjectMapper objectMapper;

  @Value("${alcohol.lookup.token-index.enabled:false}")
  private boolean tokenIndexEnabled;

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
  public Optional<List<AlcoholLookupSnapshotItem>> findIndexed(AlcoholLookupRequest request) {
    if (!Boolean.TRUE.equals(redisTemplate.hasKey(INDEX_KEYS_KEY))) {
      return Optional.empty();
    }

    List<String> indexKeys = indexKeys(request);
    if (indexKeys.isEmpty()) {
      return Optional.empty();
    }
    if (indexKeys.stream().anyMatch(key -> !Boolean.TRUE.equals(redisTemplate.hasKey(key)))) {
      return Optional.empty();
    }

    Set<Object> alcoholIds =
        indexKeys.size() == 1
            ? redisTemplate.opsForSet().members(indexKeys.get(0))
            : redisTemplate.opsForSet().intersect(indexKeys);
    if (alcoholIds == null || alcoholIds.isEmpty()) {
      return Optional.of(List.of());
    }

    return hydrateItems(alcoholIds);
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
      if (tokenIndexEnabled) {
        replaceIndexedSnapshot(items);
      } else {
        deleteIndexedSnapshot();
      }
      redisTemplate.opsForValue().set(LOOKUP_VERSION_KEY, UUID.randomUUID().toString());
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Alcohol lookup snapshot 직렬화에 실패했습니다.", e);
    }
  }

  private void replaceIndexedSnapshot(List<AlcoholLookupSnapshotItem> items)
      throws JsonProcessingException {
    deleteIndexedSnapshot();
    for (AlcoholLookupSnapshotItem item : items) {
      String alcoholId = item.alcoholId().toString();
      redisTemplate
          .opsForHash()
          .put(LOOKUP_ITEM_HASH_KEY, alcoholId, objectMapper.writeValueAsString(item));
      for (String key : indexKeys(item)) {
        redisTemplate.opsForSet().add(key, alcoholId);
        redisTemplate.opsForSet().add(INDEX_KEYS_KEY, key);
      }
    }
  }

  private void deleteIndexedSnapshot() {
    Set<Object> indexKeys = redisTemplate.opsForSet().members(INDEX_KEYS_KEY);
    if (indexKeys != null && !indexKeys.isEmpty()) {
      redisTemplate.delete(indexKeys.stream().map(Object::toString).toList());
    }
    redisTemplate.delete(INDEX_KEYS_KEY);
    redisTemplate.delete(LOOKUP_ITEM_HASH_KEY);
  }

  private Optional<List<AlcoholLookupSnapshotItem>> hydrateItems(Collection<Object> alcoholIds) {
    List<Object> hashKeys =
        alcoholIds.stream()
            .map(Object::toString)
            .sorted(Comparator.comparingLong(Long::parseLong))
            .map(value -> (Object) value)
            .toList();
    List<Object> values = redisTemplate.opsForHash().multiGet(LOOKUP_ITEM_HASH_KEY, hashKeys);
    if (values == null
        || values.size() != hashKeys.size()
        || values.stream().anyMatch(Objects::isNull)) {
      log.warn("Alcohol lookup indexed snapshot hydrate 실패. full snapshot 경로로 fallback합니다.");
      return Optional.empty();
    }

    List<AlcoholLookupSnapshotItem> items = new ArrayList<>(values.size());
    try {
      for (Object value : values) {
        items.add(objectMapper.readValue(value.toString(), AlcoholLookupSnapshotItem.class));
      }
      return Optional.of(items);
    } catch (JsonProcessingException e) {
      log.warn("Alcohol lookup indexed snapshot 역직렬화 실패. full snapshot 경로로 fallback합니다.", e);
      return Optional.empty();
    }
  }

  private List<String> indexKeys(AlcoholLookupRequest request) {
    LinkedHashSet<String> keys = new LinkedHashSet<>();
    tokenize(request.keyword()).map(this::tokenKey).forEach(keys::add);
    if (request.categoryGroup() != null) {
      keys.add(categoryKey(request.categoryGroup().name()));
    }
    if (request.regionId() != null) {
      keys.add(regionKey(request.regionId()));
    }
    if (request.distilleryId() != null) {
      keys.add(distilleryKey(request.distilleryId()));
    }
    return List.copyOf(keys);
  }

  private List<String> indexKeys(AlcoholLookupSnapshotItem item) {
    LinkedHashSet<String> keys = new LinkedHashSet<>();
    tokenize(item.normalizedSearchText())
        .flatMap(this::prefixes)
        .map(this::tokenKey)
        .forEach(keys::add);
    if (item.categoryGroup() != null) {
      keys.add(categoryKey(item.categoryGroup().name()));
    }
    if (item.regionId() != null) {
      keys.add(regionKey(item.regionId()));
    }
    if (item.distilleryId() != null) {
      keys.add(distilleryKey(item.distilleryId()));
    }
    return List.copyOf(keys);
  }

  private Stream<String> tokenize(String text) {
    if (text == null || text.isBlank()) {
      return Stream.empty();
    }
    return Stream.of(text.toLowerCase(Locale.ROOT).split("\\s+")).filter(token -> !token.isBlank());
  }

  private Stream<String> prefixes(String token) {
    if (token.length() == 1) {
      return Stream.of(token);
    }
    return java.util.stream.IntStream.rangeClosed(1, token.length())
        .mapToObj(index -> token.substring(0, index));
  }

  private String tokenKey(String token) {
    return INDEX_KEY_PREFIX + "token:" + token;
  }

  private String categoryKey(String category) {
    return INDEX_KEY_PREFIX + "category:" + category;
  }

  private String regionKey(Long regionId) {
    return INDEX_KEY_PREFIX + "region:" + regionId;
  }

  private String distilleryKey(Long distilleryId) {
    return INDEX_KEY_PREFIX + "distillery:" + distilleryId;
  }
}
