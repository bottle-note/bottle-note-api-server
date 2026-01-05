package app.bottlenote.common.file.dto.request;

import app.bottlenote.common.file.domain.ImageUploadLog;
import app.bottlenote.common.file.domain.ImageUploadStatus;
import lombok.Builder;

@Builder
public record ImageUploadLogRequest(
    Long userId,
    String imageKey,
    String viewUrl,
    String rootPath,
    String bucketName,
    String originalFileName,
    String contentType,
    Long contentLength) {

  public ImageUploadLog toEntity() {
    return ImageUploadLog.builder()
        .userId(userId)
        .imageKey(imageKey)
        .viewUrl(viewUrl)
        .rootPath(rootPath)
        .bucketName(bucketName)
        .originalFileName(originalFileName)
        .contentType(contentType)
        .contentLength(contentLength)
        .status(ImageUploadStatus.PENDING)
        .build();
  }
}
