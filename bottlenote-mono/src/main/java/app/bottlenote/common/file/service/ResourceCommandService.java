package app.bottlenote.common.file.service;

import app.bottlenote.common.file.constant.ResourceEventType;
import app.bottlenote.common.file.domain.ResourceLog;
import app.bottlenote.common.file.domain.ResourceLogRepository;
import app.bottlenote.common.file.dto.request.ResourceLogRequest;
import app.bottlenote.common.file.dto.response.ResourceLogItem;
import app.bottlenote.common.file.dto.response.ResourceLogResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceCommandService {

  private static final String RESOURCE_TYPE_IMAGE = "IMAGE";

  private final ResourceLogRepository resourceLogRepository;

  @Async
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public CompletableFuture<ResourceLogResponse> saveImageResourceCreated(
      ResourceLogRequest request) {
    ResourceLog entity =
        ResourceLog.builder()
            .userId(request.userId())
            .resourceKey(request.resourceKey())
            .resourceType(RESOURCE_TYPE_IMAGE)
            .eventType(ResourceEventType.CREATED)
            .viewUrl(request.viewUrl())
            .rootPath(request.rootPath())
            .bucketName(request.bucketName())
            .build();
    ResourceLog saved = resourceLogRepository.save(entity);
    log.info(
        "이미지 리소스 생성 로그 저장 - resourceKey: {}, userId: {}",
        saved.getResourceKey(),
        saved.getUserId());
    return CompletableFuture.completedFuture(toResponse(saved));
  }

  @Async
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public CompletableFuture<Optional<ResourceLogResponse>> activateImageResource(
      String resourceKey, Long referenceId, String referenceType) {
    ResourceLog entity =
        ResourceLog.builder()
            .userId(getUserIdFromLatestLog(resourceKey))
            .resourceKey(resourceKey)
            .resourceType(RESOURCE_TYPE_IMAGE)
            .eventType(ResourceEventType.ACTIVATED)
            .referenceId(referenceId)
            .referenceType(referenceType)
            .viewUrl(getViewUrlFromLatestLog(resourceKey))
            .build();
    ResourceLog saved = resourceLogRepository.save(entity);
    log.info("이미지 리소스 활성화 로그 저장 - resourceKey: {}, referenceId: {}", resourceKey, referenceId);
    return CompletableFuture.completedFuture(Optional.of(toResponse(saved)));
  }

  @Async
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public CompletableFuture<Optional<ResourceLogResponse>> invalidateImageResource(
      String resourceKey) {
    ResourceLog entity =
        ResourceLog.builder()
            .userId(getUserIdFromLatestLog(resourceKey))
            .resourceKey(resourceKey)
            .resourceType(RESOURCE_TYPE_IMAGE)
            .eventType(ResourceEventType.INVALIDATED)
            .viewUrl(getViewUrlFromLatestLog(resourceKey))
            .build();
    ResourceLog saved = resourceLogRepository.save(entity);
    log.info("이미지 리소스 무효화 로그 저장 - resourceKey: {}", resourceKey);
    return CompletableFuture.completedFuture(Optional.of(toResponse(saved)));
  }

  @Transactional(readOnly = true)
  public Optional<ResourceLogResponse> findLatestByResourceKey(String resourceKey) {
    return resourceLogRepository.findLatestByResourceKey(resourceKey).map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public List<ResourceLogItem> findByEventTypeAndCreateAtBefore(
      ResourceEventType eventType, LocalDateTime dateTime) {
    return resourceLogRepository.findByEventTypeAndCreateAtBefore(eventType, dateTime).stream()
        .map(this::toItem)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ResourceLogResponse> findByResourceKey(String resourceKey) {
    return resourceLogRepository.findByResourceKey(resourceKey).stream()
        .map(this::toResponse)
        .toList();
  }

  private Long getUserIdFromLatestLog(String resourceKey) {
    return resourceLogRepository
        .findLatestByResourceKey(resourceKey)
        .map(ResourceLog::getUserId)
        .orElse(null);
  }

  private String getViewUrlFromLatestLog(String resourceKey) {
    return resourceLogRepository
        .findLatestByResourceKey(resourceKey)
        .map(ResourceLog::getViewUrl)
        .orElse(null);
  }

  private ResourceLogItem toItem(ResourceLog log) {
    return ResourceLogItem.builder()
        .id(log.getId())
        .userId(log.getUserId())
        .resourceKey(log.getResourceKey())
        .resourceType(log.getResourceType())
        .eventType(log.getEventType())
        .referenceId(log.getReferenceId())
        .referenceType(log.getReferenceType())
        .viewUrl(log.getViewUrl())
        .rootPath(log.getRootPath())
        .bucketName(log.getBucketName())
        .createAt(log.getCreateAt())
        .build();
  }

  private ResourceLogResponse toResponse(ResourceLog log) {
    return ResourceLogResponse.builder()
        .id(log.getId())
        .userId(log.getUserId())
        .resourceKey(log.getResourceKey())
        .resourceType(log.getResourceType())
        .eventType(log.getEventType())
        .referenceId(log.getReferenceId())
        .referenceType(log.getReferenceType())
        .viewUrl(log.getViewUrl())
        .rootPath(log.getRootPath())
        .bucketName(log.getBucketName())
        .createAt(log.getCreateAt())
        .build();
  }
}
