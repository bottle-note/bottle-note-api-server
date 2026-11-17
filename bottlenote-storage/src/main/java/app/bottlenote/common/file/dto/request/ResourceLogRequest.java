package app.bottlenote.common.file.dto.request;

import lombok.Builder;

@Builder
public record ResourceLogRequest(
    Long userId, String resourceKey, String viewUrl, String rootPath, String bucketName) {}
