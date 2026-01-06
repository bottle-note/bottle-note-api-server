package app.bottlenote.common.file.dto.response;

import app.bottlenote.common.file.constant.ResourceEventType;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record ResourceLogResponse(
    Long id,
    Long userId,
    String resourceKey,
    String resourceType,
    ResourceEventType eventType,
    Long referenceId,
    String referenceType,
    String viewUrl,
    String rootPath,
    String bucketName,
    LocalDateTime createAt) {}
