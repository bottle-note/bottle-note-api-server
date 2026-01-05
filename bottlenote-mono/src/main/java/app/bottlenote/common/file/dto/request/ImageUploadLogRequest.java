package app.bottlenote.common.file.dto.request;

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
    Long contentLength) {}
