package app.bottlenote.common.file.service;

import app.bottlenote.common.file.domain.ImageUploadLog;
import app.bottlenote.common.file.domain.ImageUploadLogRepository;
import app.bottlenote.common.file.domain.ImageUploadStatus;
import app.bottlenote.common.file.dto.ImageUploadLogItem;
import app.bottlenote.common.file.dto.request.ImageUploadLogRequest;
import app.bottlenote.common.file.dto.response.ImageUploadLogResponse;
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
public class ImageUploadLogService {

  private final ImageUploadLogRepository imageUploadLogRepository;

  /**
   * 비동기로 이미지 로그를 저장한다.
   *
   * @param request 이미지 업로드 로그 요청
   * @return 저장된 이미지 업로드 로그 응답
   */
  @Async
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public CompletableFuture<ImageUploadLogResponse> saveAsync(ImageUploadLogRequest request) {
    ImageUploadLog saved = imageUploadLogRepository.save(request.toEntity());
    log.info("이미지 업로드 로그 저장 완료 - imageKey: {}, userId: {}", saved.getImageKey(), saved.getUserId());
    return CompletableFuture.completedFuture(toResponse(saved));
  }

  /**
   * 이미지 로그 정보를 조회한다.
   *
   * @param imageKey S3 객체 키
   * @return 이미지 업로드 로그 응답
   */
  @Transactional(readOnly = true)
  public Optional<ImageUploadLogResponse> findByImageKey(String imageKey) {
    return imageUploadLogRepository.findByImageKey(imageKey).map(this::toResponse);
  }

  /**
   * 날짜와 상태를 기준으로 미확정된 로그 목록을 조회한다.
   *
   * @param status 조회할 상태
   * @param dateTime 기준 날짜 (이 날짜 이전에 생성된 로그)
   * @return 이미지 업로드 로그 아이템 목록
   */
  @Transactional(readOnly = true)
  public List<ImageUploadLogItem> findUnconfirmedLogs(
      ImageUploadStatus status, LocalDateTime dateTime) {
    return imageUploadLogRepository.findByStatusAndCreateAtBefore(status, dateTime).stream()
        .map(this::toItem)
        .toList();
  }

  private ImageUploadLogItem toItem(ImageUploadLog log) {
    return ImageUploadLogItem.builder()
        .id(log.getId())
        .userId(log.getUserId())
        .imageKey(log.getImageKey())
        .viewUrl(log.getViewUrl())
        .status(log.getStatus())
        .referenceId(log.getReferenceId())
        .referenceType(log.getReferenceType())
        .rootPath(log.getRootPath())
        .bucketName(log.getBucketName())
        .createdAt(log.getCreateAt())
        .confirmedAt(log.getConfirmedAt())
        .build();
  }

  private ImageUploadLogResponse toResponse(ImageUploadLog log) {
    return ImageUploadLogResponse.builder()
        .id(log.getId())
        .userId(log.getUserId())
        .imageKey(log.getImageKey())
        .viewUrl(log.getViewUrl())
        .status(log.getStatus())
        .referenceId(log.getReferenceId())
        .referenceType(log.getReferenceType())
        .rootPath(log.getRootPath())
        .contentType(log.getContentType())
        .contentLength(log.getContentLength())
        .originalFileName(log.getOriginalFileName())
        .bucketName(log.getBucketName())
        .etag(log.getEtag())
        .createdAt(log.getCreateAt())
        .confirmedAt(log.getConfirmedAt())
        .build();
  }

  /**
   * 비동기로 이미지 상태를 업데이트한다.
   *
   * @param imageKey S3 객체 키
   * @param referenceId 연결된 엔티티 ID
   * @param referenceType 연결 타입
   * @return 업데이트된 이미지 업로드 로그 응답
   */
  @Async
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public CompletableFuture<Optional<ImageUploadLogResponse>> confirmAsync(
      String imageKey, Long referenceId, String referenceType) {
    return imageUploadLogRepository
        .findByImageKey(imageKey)
        .map(
            uploadLog -> {
              uploadLog.confirm(referenceId, referenceType);
              log.info(
                  "이미지 상태 확정 완료 - imageKey: {}, referenceId: {}, referenceType: {}",
                  imageKey,
                  referenceId,
                  referenceType);
              return CompletableFuture.completedFuture(Optional.of(toResponse(uploadLog)));
            })
        .orElseGet(
            () -> {
              log.warn("이미지 로그를 찾을 수 없음 - imageKey: {}", imageKey);
              return CompletableFuture.completedFuture(Optional.empty());
            });
  }
}
