package app.bottlenote.common.file.dto;

import app.bottlenote.common.file.domain.ImageUploadStatus;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record ImageUploadLogItem(
    Long id,
    Long userId,
    String imageKey,
    String viewUrl,
    ImageUploadStatus status,
    Long referenceId,
    String referenceType,
    String rootPath,
    String bucketName,
    LocalDateTime createdAt,
    LocalDateTime confirmedAt) {}
