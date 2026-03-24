package app.bottlenote.common.file;

import static app.bottlenote.common.file.exception.FileExceptionCode.EXPIRY_TIME_RANGE_INVALID;
import static app.bottlenote.common.file.exception.FileExceptionCode.UNSUPPORTED_CONTENT_TYPE;
import static java.time.format.DateTimeFormatter.ofPattern;

import app.bottlenote.common.file.exception.FileException;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public interface PreSignUrlProvider {

  Map<String, String> ALLOWED_CONTENT_TYPES =
      Map.of(
          "image/jpeg", "jpg",
          "image/png", "png",
          "image/webp", "webp",
          "video/mp4", "mp4");

  String PATH_DELIMITER = "/";
  String KEY_DELIMITER = "-";

  /**
   * PreSignUrl을 생성한다.
   *
   * @param imageKey the image key
   * @param contentType 업로드할 파일의 Content-Type
   * @return the string
   */
  String generatePreSignUrl(String imageKey, String contentType);

  /**
   * ViewUrl을 생성한다. cloud front url 에 s3 key를 조합해 반환한다. 실제 오브젝트를 조회하기 위해 사용된다.
   *
   * @param cloudFrontUrl 배포 클라우드 프론트 URL
   * @param imageKey s3 오브젝트 루트 경로
   * @return the string
   */
  String generateViewUrl(String cloudFrontUrl, String imageKey);

  /**
   * 루트 경로를 포함한 오브젝트 키를 생성한다. contentType에 따라 확장자를 결정한다.
   *
   * @param rootPath 저장할 루트 경로
   * @param index 업로드 순번
   * @param contentType 업로드할 파일의 Content-Type
   * @return 생성된 오브젝트 키
   */
  default String getImageKey(String rootPath, Long index, String contentType) {
    if (rootPath.startsWith(PATH_DELIMITER)) {
      rootPath = rootPath.substring(1);
    }
    if (rootPath.endsWith(PATH_DELIMITER)) {
      rootPath = rootPath.substring(0, rootPath.length() - 1);
    }

    String extension = ALLOWED_CONTENT_TYPES.get(contentType);
    if (extension == null) {
      throw new FileException(UNSUPPORTED_CONTENT_TYPE);
    }

    String uploadAt = LocalDate.now().format(ofPattern("yyyyMMdd"));
    String imageId = index + KEY_DELIMITER + UUID.randomUUID() + "." + extension;

    return rootPath + PATH_DELIMITER + uploadAt + PATH_DELIMITER + imageId;
  }

  /**
   * 업로드 가능 만료 시간을 계산하여 반환한다.
   *
   * @param minutes 만료 시간 (분)
   * @return 만료 시간 객체(Calendar)
   */
  default Calendar getUploadExpiryTime(Integer minutes) {
    if (Objects.isNull(minutes)) minutes = 5;

    if (minutes < 1 || minutes > 10) {
      throw new FileException(EXPIRY_TIME_RANGE_INVALID);
    }

    Calendar expiryTime = Calendar.getInstance();
    expiryTime.add(Calendar.MINUTE, minutes);
    return expiryTime;
  }
}
