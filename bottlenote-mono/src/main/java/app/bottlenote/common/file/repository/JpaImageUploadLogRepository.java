package app.bottlenote.common.file.repository;

import app.bottlenote.common.annotation.JpaRepositoryImpl;
import app.bottlenote.common.file.domain.ImageUploadLog;
import app.bottlenote.common.file.domain.ImageUploadLogRepository;
import app.bottlenote.common.file.domain.ImageUploadStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@JpaRepositoryImpl
public interface JpaImageUploadLogRepository
    extends ImageUploadLogRepository, JpaRepository<ImageUploadLog, Long> {

  Optional<ImageUploadLog> findByImageKey(String imageKey);

  List<ImageUploadLog> findByUserId(Long userId);

  @Query("SELECT i FROM image_upload_log i WHERE i.status = :status AND i.createAt < :dateTime")
  List<ImageUploadLog> findByStatusAndCreateAtBefore(
      @Param("status") ImageUploadStatus status, @Param("dateTime") LocalDateTime dateTime);

  List<ImageUploadLog> findByReferenceIdAndReferenceType(Long referenceId, String referenceType);
}
