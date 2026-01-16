package app.bottlenote.common.file.domain;

import app.bottlenote.common.annotation.DomainRepository;
import app.bottlenote.common.file.constant.ResourceEventType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@DomainRepository
public interface ResourceLogRepository {

  ResourceLog save(ResourceLog resourceLog);

  Optional<ResourceLog> findById(Long id);

  Optional<ResourceLog> findByResourceKey(String resourceKey);

  List<ResourceLog> findByUserId(Long userId);

  List<ResourceLog> findByEventTypeAndCreateAtBefore(
      ResourceEventType eventType, LocalDateTime dateTime);

  List<ResourceLog> findByReferenceIdAndReferenceType(Long referenceId, String referenceType);

  void delete(ResourceLog resourceLog);
}
