package app.bottlenote.common.file.service;

import app.bottlenote.common.annotation.ThirdPartyService;
import app.bottlenote.common.file.PreSignUrlProvider;
import app.bottlenote.common.file.dto.request.ImageUploadRequest;
import app.bottlenote.common.file.dto.request.ResourceLogRequest;
import app.bottlenote.common.file.dto.response.ImageUploadItem;
import app.bottlenote.common.file.dto.response.ImageUploadResponse;
import app.bottlenote.global.security.SecurityContextUtil;
import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@ThirdPartyService
public class ImageUploadService implements PreSignUrlProvider {

  private static final Integer EXPIRY_TIME = 5;
  private final ResourceCommandService resourceCommandService;
  private final AmazonS3 amazonS3;
  private final String imageBucketName;
  private final String cloudFrontUrl;

  public ImageUploadService(
      ResourceCommandService resourceCommandService,
      AmazonS3 amazonS3,
      @Value("${amazon.aws.bucket}") String imageBucketName,
      @Value("${amazon.aws.cloudFrontUrl}") String cloudFrontUrl) {
    this.resourceCommandService = resourceCommandService;
    this.amazonS3 = amazonS3;
    this.imageBucketName = imageBucketName;
    this.cloudFrontUrl = cloudFrontUrl;
  }

  /**
   * 업로드용 인증 URL을 생성한다.
   *
   * @param request 업로드랑 루트 경로 + 업로드할 사이즈
   * @return the 생성된 이미지 업로드 정보
   */
  public ImageUploadResponse getPreSignUrl(ImageUploadRequest request) {
    String rootPath = request.rootPath();
    Long uploadSize = request.uploadSize();
    List<ImageUploadItem> keys = new ArrayList<>();

    for (long index = 1; index <= uploadSize; index++) {
      String imageKey = getImageKey(rootPath, index);
      String preSignUrl = generatePreSignUrl(imageKey);
      String viewUrl = generateViewUrl(cloudFrontUrl, imageKey);
      keys.add(
          ImageUploadItem.builder().order(index).viewUrl(viewUrl).uploadUrl(preSignUrl).build());
    }
    saveImageUploadLogs(rootPath, keys);

    log.info(
        "S3 PreSignedURL 생성 완료 - rootPath: {}, uploadSize: {}, bucket: {}, expiryTime: {}분",
        rootPath,
        uploadSize,
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
    return cloudFrontUrl + PATH_DELIMITER + imageKey;
  }

  @Override
  public String generatePreSignUrl(String imageKey) {
    Calendar uploadExpiryTime = getUploadExpiryTime(EXPIRY_TIME);
    return amazonS3
        .generatePresignedUrl(imageBucketName, imageKey, uploadExpiryTime.getTime(), HttpMethod.PUT)
        .toString();
  }

  private void saveImageUploadLogs(String rootPath, List<ImageUploadItem> items) {
    SecurityContextUtil.getUserIdByContext()
        .ifPresent(
            userId ->
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
                    }));
  }

  private String extractImageKey(String viewUrl) {
    int lastSlashOfCloudFront = cloudFrontUrl.length() + 1;
    return viewUrl.substring(lastSlashOfCloudFront);
  }
}
