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
    Optional<ResourceLog> resourceLogOpt = resourceLogRepository.findByResourceKey(resourceKey);

    if (resourceLogOpt.isEmpty()) {
      log.warn("리소스 로그를 찾을 수 없음 - resourceKey: {}", resourceKey);
      return CompletableFuture.completedFuture(Optional.empty());
    }

    ResourceLog resourceLog = resourceLogOpt.get();

    if (resourceLog.isActivated()) {
      log.info("이미 활성화된 리소스 스킵 - resourceKey: {}", resourceKey);
      return CompletableFuture.completedFuture(Optional.empty());
    }

    resourceLog.activate(referenceId, referenceType);
    ResourceLog saved = resourceLogRepository.save(resourceLog);
    log.info("이미지 리소스 활성화 - resourceKey: {}, referenceId: {}", resourceKey, referenceId);
    return CompletableFuture.completedFuture(Optional.of(toResponse(saved)));
  }

  @Async
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public CompletableFuture<Optional<ResourceLogResponse>> invalidateImageResource(
      String resourceKey) {
    Optional<ResourceLog> resourceLogOpt = resourceLogRepository.findByResourceKey(resourceKey);

    if (resourceLogOpt.isEmpty()) {
      log.warn("리소스 로그를 찾을 수 없음 - resourceKey: {}", resourceKey);
      return CompletableFuture.completedFuture(Optional.empty());
    }

    ResourceLog resourceLog = resourceLogOpt.get();

    if (resourceLog.isInvalidated()) {
      log.info("이미 무효화된 리소스 스킵 - resourceKey: {}", resourceKey);
      return CompletableFuture.completedFuture(Optional.empty());
    }

    resourceLog.invalidate();
    ResourceLog saved = resourceLogRepository.save(resourceLog);
    log.info("이미지 리소스 무효화 - resourceKey: {}", resourceKey);
    return CompletableFuture.completedFuture(Optional.of(toResponse(saved)));
  }

  @Async
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public CompletableFuture<Optional<ResourceLogResponse>> deleteImageResource(String resourceKey) {
    Optional<ResourceLog> resourceLogOpt = resourceLogRepository.findByResourceKey(resourceKey);

    if (resourceLogOpt.isEmpty()) {
      log.warn("리소스 로그를 찾을 수 없음 - resourceKey: {}", resourceKey);
      return CompletableFuture.completedFuture(Optional.empty());
    }

    ResourceLog resourceLog = resourceLogOpt.get();

    if (!resourceLog.canTransitionTo(ResourceEventType.DELETED)) {
      log.info(
          "삭제 상태로 전이 불가 - resourceKey: {}, 현재 상태: {}", resourceKey, resourceLog.getEventType());
      return CompletableFuture.completedFuture(Optional.empty());
    }

    resourceLog.markDeleted();
    ResourceLog saved = resourceLogRepository.save(resourceLog);
    log.info("이미지 리소스 삭제 - resourceKey: {}", resourceKey);
    return CompletableFuture.completedFuture(Optional.of(toResponse(saved)));
  }

  @Transactional(readOnly = true)
  public Optional<ResourceLogResponse> findByResourceKey(String resourceKey) {
    return resourceLogRepository.findByResourceKey(resourceKey).map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public List<ResourceLogItem> findByEventTypeAndCreateAtBefore(
      ResourceEventType eventType, LocalDateTime dateTime) {
    return resourceLogRepository.findByEventTypeAndCreateAtBefore(eventType, dateTime).stream()
        .map(this::toItem)
        .toList();
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
        .lastModifyAt(log.getLastModifyAt())
        .build();
  }
}
