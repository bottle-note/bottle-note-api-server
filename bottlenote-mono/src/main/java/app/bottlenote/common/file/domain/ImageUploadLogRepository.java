package app.bottlenote.common.file.domain;

import app.bottlenote.common.annotation.DomainRepository;
import app.bottlenote.common.file.constant.ImageUploadStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@DomainRepository
public interface ImageUploadLogRepository {

  ImageUploadLog save(ImageUploadLog imageUploadLog);

  Optional<ImageUploadLog> findById(Long id);

  Optional<ImageUploadLog> findByImageKey(String imageKey);

  List<ImageUploadLog> findByUserId(Long userId);

  List<ImageUploadLog> findByStatusAndCreateAtBefore(
      ImageUploadStatus status, LocalDateTime dateTime);

  List<ImageUploadLog> findByReferenceIdAndReferenceType(Long referenceId, String referenceType);

  void delete(ImageUploadLog imageUploadLog);
}
