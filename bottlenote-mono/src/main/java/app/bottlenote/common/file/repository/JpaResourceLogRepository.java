package app.bottlenote.common.file.repository;

import app.bottlenote.common.annotation.JpaRepositoryImpl;
import app.bottlenote.common.file.constant.ResourceEventType;
import app.bottlenote.common.file.domain.ResourceLog;
import app.bottlenote.common.file.domain.ResourceLogRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@JpaRepositoryImpl
public interface JpaResourceLogRepository
    extends ResourceLogRepository, JpaRepository<ResourceLog, Long> {

  Optional<ResourceLog> findByResourceKey(String resourceKey);

  List<ResourceLog> findByUserId(Long userId);

  @Query("SELECT r FROM resource_log r WHERE r.eventType = :eventType AND r.createAt < :dateTime")
  List<ResourceLog> findByEventTypeAndCreateAtBefore(
      @Param("eventType") ResourceEventType eventType, @Param("dateTime") LocalDateTime dateTime);

  List<ResourceLog> findByReferenceIdAndReferenceType(Long referenceId, String referenceType);
}
