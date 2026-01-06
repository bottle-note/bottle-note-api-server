package app.bottlenote.common.file.upload.fixture;

import app.bottlenote.common.file.constant.ResourceEventType;
import app.bottlenote.common.file.domain.ResourceLog;
import app.bottlenote.common.file.domain.ResourceLogRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
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

  @Override
  public ResourceLog save(ResourceLog resourceLog) {
    Long id = (Long) ReflectionTestUtils.getField(resourceLog, "id");
    if (id == null) {
      id = database.size() + 1L;
      ReflectionTestUtils.setField(resourceLog, "id", id);
    }
    database.put(id, resourceLog);
    log.info("[InMemory] resourceLog repository save = {}", resourceLog);
    return resourceLog;
  }

  @Override
  public Optional<ResourceLog> findById(Long id) {
    return Optional.ofNullable(database.get(id));
  }

  @Override
  public List<ResourceLog> findByResourceKey(String resourceKey) {
    return database.values().stream()
        .filter(log -> log.getResourceKey().equals(resourceKey))
        .toList();
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
  public Optional<ResourceLog> findLatestByResourceKey(String resourceKey) {
    return database.values().stream()
        .filter(log -> log.getResourceKey().equals(resourceKey))
        .max(Comparator.comparing(ResourceLog::getCreateAt));
  }

  @Override
  public void delete(ResourceLog resourceLog) {
    database.remove(resourceLog.getId());
  }

  public void clear() {
    database.clear();
  }

  public List<ResourceLog> findAll() {
    return List.copyOf(database.values());
  }
}
