package app.bottlenote.common.file.service;

import app.bottlenote.common.annotation.ThirdPartyService;
import app.bottlenote.common.file.PreSignUrlProvider;
import app.bottlenote.common.file.dto.request.ImageUploadRequest;
import app.bottlenote.common.file.dto.request.ResourceLogRequest;
import app.bottlenote.common.file.dto.response.ImageUploadItem;
import app.bottlenote.common.file.dto.response.ImageUploadResponse;
import app.bottlenote.global.security.SecurityContextUtil;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Slf4j
@ThirdPartyService
public class ImageUploadService implements PreSignUrlProvider {

  private static final Integer EXPIRY_TIME = 5;
  private final ResourceCommandService resourceCommandService;
  private final S3Presigner s3Presigner;
  private final String imageBucketName;
  private final String cloudFrontUrl;

  public ImageUploadService(
      ResourceCommandService resourceCommandService,
      S3Presigner s3Presigner,
      @Value("${amazon.aws.bucket}") String imageBucketName,
      @Value("${amazon.aws.cloudFrontUrl}") String cloudFrontUrl) {
    this.resourceCommandService = resourceCommandService;
    this.s3Presigner = s3Presigner;
    this.imageBucketName = imageBucketName;
    this.cloudFrontUrl = normalizeCloudFrontUrl(cloudFrontUrl);
  }

  /**
   * 업로드용 인증 URL을 생성한다.
   *
   * @param request 업로드랑 루트 경로 + 업로드할 사이즈
   * @return the 생성된 이미지 업로드 정보
   */
  public ImageUploadResponse getPreSignUrl(ImageUploadRequest request) {
    List<ImageUploadItem> keys = generatePreSignUrls(request);
    saveImageUploadLogs(request.rootPath(), keys);
    return buildResponse(keys);
  }

  /**
   * 어드민용 업로드 인증 URL을 생성한다.
   *
   * @param adminId 어드민 사용자 ID
   * @param request 업로드 루트 경로 + 업로드할 사이즈
   * @return the 생성된 이미지 업로드 정보
   */
  public ImageUploadResponse getPreSignUrlForAdmin(Long adminId, ImageUploadRequest request) {
    List<ImageUploadItem> keys = generatePreSignUrls(request);
    saveResourceLogs(request.rootPath(), keys, adminId);
    return buildResponse(keys);
  }

  private List<ImageUploadItem> generatePreSignUrls(ImageUploadRequest request) {
    String rootPath = request.rootPath();
    Long uploadSize = request.uploadSize();
    String contentType = request.contentType();
    List<ImageUploadItem> keys = new ArrayList<>();

    for (long index = 1; index <= uploadSize; index++) {
      String imageKey = getImageKey(rootPath, index, contentType);
      String preSignUrl = generatePreSignUrl(imageKey, contentType);
      String viewUrl = generateViewUrl(cloudFrontUrl, imageKey);
      keys.add(
          ImageUploadItem.builder().order(index).viewUrl(viewUrl).uploadUrl(preSignUrl).build());
    }
    return keys;
  }

  private ImageUploadResponse buildResponse(List<ImageUploadItem> keys) {
    log.info(
        "S3 PreSignedURL 생성 완료 - uploadSize: {}, bucket: {}, expiryTime: {}분",
        keys.size(),
        imageBucketName,
        EXPIRY_TIME);

    return ImageUploadResponse.builder()
        .bucketName(imageBucketName)
        .expiryTime(EXPIRY_TIME)
        .uploadSize(keys.size())
        .imageUploadInfo(keys)
        .build();
  }

  @Override
  public String generateViewUrl(String cloudFrontUrl, String imageKey) {
    return normalizeCloudFrontUrl(cloudFrontUrl) + PATH_DELIMITER + imageKey;
  }

  @Override
  public String generatePreSignUrl(String imageKey, String contentType) {
    PutObjectRequest putObjectRequest =
        PutObjectRequest.builder()
            .bucket(imageBucketName)
            .key(imageKey)
            .contentType(contentType)
            .build();
    PutObjectPresignRequest presignRequest =
        PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(EXPIRY_TIME))
            .putObjectRequest(putObjectRequest)
            .build();
    return s3Presigner.presignPutObject(presignRequest).url().toString();
  }

  private void saveImageUploadLogs(String rootPath, List<ImageUploadItem> items) {
    SecurityContextUtil.getUserIdByContext()
        .ifPresent(userId -> saveResourceLogs(rootPath, items, userId));
  }

  private void saveResourceLogs(String rootPath, List<ImageUploadItem> items, Long userId) {
    items.forEach(
        item -> {
          String imageKey = extractImageKey(item.viewUrl());
          ResourceLogRequest logRequest =
              ResourceLogRequest.builder()
                  .userId(userId)
                  .resourceKey(imageKey)
                  .viewUrl(item.viewUrl())
                  .rootPath(rootPath)
                  .bucketName(imageBucketName)
                  .build();
          resourceCommandService.saveImageResourceCreated(logRequest);
        });
  }

  private String extractImageKey(String viewUrl) {
    int lastSlashOfCloudFront = cloudFrontUrl.length() + 1;
    return viewUrl.substring(lastSlashOfCloudFront);
  }

  private String normalizeCloudFrontUrl(String url) {
    String normalized = url;
    while (normalized.endsWith(PATH_DELIMITER)) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    return normalized;
  }
}
