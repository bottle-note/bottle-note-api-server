package app.bottlenote.common.file.dto.response;

import app.bottlenote.common.file.constant.ImageUploadStatus;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record ImageUploadLogResponse(
    Long id,
    Long userId,
    String imageKey,
    String viewUrl,
    ImageUploadStatus status,
    Long referenceId,
    String referenceType,
    String rootPath,
    String contentType,
    Long contentLength,
    String originalFileName,
    String bucketName,
    String etag,
    LocalDateTime createdAt,
    LocalDateTime confirmedAt) {}
