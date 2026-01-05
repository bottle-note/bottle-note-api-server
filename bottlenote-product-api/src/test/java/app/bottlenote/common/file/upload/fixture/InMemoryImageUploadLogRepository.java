package app.bottlenote.common.file.upload.fixture;

import app.bottlenote.common.file.domain.ImageUploadLog;
import app.bottlenote.common.file.domain.ImageUploadLogRepository;
import app.bottlenote.common.file.domain.ImageUploadStatus;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryImageUploadLogRepository implements ImageUploadLogRepository {

  private static final Logger log = LogManager.getLogger(InMemoryImageUploadLogRepository.class);
  Map<Long, ImageUploadLog> database = new HashMap<>();

  @Override
  public ImageUploadLog save(ImageUploadLog imageUploadLog) {
    Long id = (Long) ReflectionTestUtils.getField(imageUploadLog, "id");
    if (id != null && database.containsKey(id)) {
      database.put(id, imageUploadLog);
    } else {
      id = database.size() + 1L;
      database.put(id, imageUploadLog);
      ReflectionTestUtils.setField(imageUploadLog, "id", id);
    }
    log.info("[InMemory] imageUploadLog repository save = {}", imageUploadLog);
    return imageUploadLog;
  }

  @Override
  public Optional<ImageUploadLog> findById(Long id) {
    return Optional.ofNullable(database.get(id));
  }

  @Override
  public Optional<ImageUploadLog> findByImageKey(String imageKey) {
    return database.values().stream()
        .filter(uploadLog -> uploadLog.getImageKey().equals(imageKey))
        .findFirst();
  }

  @Override
  public List<ImageUploadLog> findByUserId(Long userId) {
    return database.values().stream()
        .filter(uploadLog -> uploadLog.getUserId().equals(userId))
        .toList();
  }

  @Override
  public List<ImageUploadLog> findByStatusAndCreateAtBefore(
      ImageUploadStatus status, LocalDateTime dateTime) {
    return database.values().stream()
        .filter(uploadLog -> uploadLog.getStatus() == status)
        .filter(
            uploadLog -> {
              LocalDateTime createAt =
                  (LocalDateTime) ReflectionTestUtils.getField(uploadLog, "createAt");
              return createAt == null || createAt.isBefore(dateTime);
            })
        .toList();
  }

  @Override
  public List<ImageUploadLog> findByReferenceIdAndReferenceType(
      Long referenceId, String referenceType) {
    return database.values().stream()
        .filter(uploadLog -> referenceId.equals(uploadLog.getReferenceId()))
        .filter(uploadLog -> referenceType.equals(uploadLog.getReferenceType()))
        .toList();
  }

  @Override
  public void delete(ImageUploadLog imageUploadLog) {
    database.remove(imageUploadLog.getId());
  }

  public void clear() {
    database.clear();
  }

  public List<ImageUploadLog> findAll() {
    return List.copyOf(database.values());
  }
}
