package app.bottlenote.common.file.upload.fixture;

import app.bottlenote.common.file.constant.ResourceEventType;
import app.bottlenote.common.file.domain.ResourceLog;
import app.bottlenote.common.file.domain.ResourceLogRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryResourceLogRepository implements ResourceLogRepository {

  private static final Logger log = LogManager.getLogger(InMemoryResourceLogRepository.class);
  private final Map<Long, ResourceLog> database = new HashMap<>();
  private final Map<String, Long> resourceKeyIndex = new HashMap<>();

  @Override
  public ResourceLog save(ResourceLog resourceLog) {
    Long id = (Long) ReflectionTestUtils.getField(resourceLog, "id");
    if (id == null) {
      id = database.size() + 1L;
      ReflectionTestUtils.setField(resourceLog, "id", id);
    }
    database.put(id, resourceLog);
    resourceKeyIndex.put(resourceLog.getResourceKey(), id);
    log.info("[InMemory] resourceLog repository save = {}", resourceLog);
    return resourceLog;
  }

  @Override
  public Optional<ResourceLog> findById(Long id) {
    return Optional.ofNullable(database.get(id));
  }

  @Override
  public Optional<ResourceLog> findByResourceKey(String resourceKey) {
    Long id = resourceKeyIndex.get(resourceKey);
    if (id == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(database.get(id));
  }

  @Override
  public List<ResourceLog> findByUserId(Long userId) {
    return database.values().stream().filter(log -> log.getUserId().equals(userId)).toList();
  }

  @Override
  public List<ResourceLog> findByEventTypeAndCreateAtBefore(
      ResourceEventType eventType, LocalDateTime dateTime) {
    return database.values().stream()
        .filter(log -> log.getEventType() == eventType)
        .filter(log -> log.getCreateAt() == null || log.getCreateAt().isBefore(dateTime))
        .toList();
  }

  @Override
  public List<ResourceLog> findByReferenceIdAndReferenceType(
      Long referenceId, String referenceType) {
    return database.values().stream()
        .filter(log -> referenceId.equals(log.getReferenceId()))
        .filter(log -> referenceType.equals(log.getReferenceType()))
        .toList();
  }

  @Override
  public void delete(ResourceLog resourceLog) {
    resourceKeyIndex.remove(resourceLog.getResourceKey());
    database.remove(resourceLog.getId());
  }

  public void clear() {
    database.clear();
    resourceKeyIndex.clear();
  }

  public List<ResourceLog> findAll() {
    return List.copyOf(database.values());
  }
}
