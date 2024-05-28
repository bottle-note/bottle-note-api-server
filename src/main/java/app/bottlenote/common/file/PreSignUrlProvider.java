package app.bottlenote.common.file;

import app.bottlenote.common.file.exception.FileException;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.Objects;
import java.util.UUID;

import static app.bottlenote.common.file.exception.FileExceptionCode.EXPIRY_TIME_RANGE_INVALID;
import static java.time.format.DateTimeFormatter.ofPattern;

public interface PreSignUrlProvider {

	String EXTENSION = "jpg";
	String PATH_DELIMITER = "/";
	String KEY_DELIMITER = "-";

	/**
	 * PreSignUrl을 생성한다.
	 *
	 * @param imageKey the image key
	 * @param index    the index
	 * @return the string
	 */
	String generatePreSignUrl(String imageKey, Long index);

	/**
	 * ViewUrl을 생성한다.
	 * cloud front url 에 s3 key를 조합해 반환한다.
	 * 실제 오브젝트를 조회하기 위해 사용된다.
	 *
	 * @param cloudFrontUrl 배포 클라우드 프론트 URL
	 * @param imageKey      s3 오브젝트 루트 경로
	 * @param index         이미지 순서 번호
	 * @return the string
	 */
	String generateViewUrl(String cloudFrontUrl, String imageKey, Long index);

	/**
	 * 업로드 가능 만료 시간을 계산하여 반환한다.
	 *
	 * @param minutes 만료 시간 (분)
	 * @return 만료 시간 객체(Calendar)
	 */
	default Calendar getUploadExpiryTime(Integer minutes) {
		if (Objects.isNull(minutes))
			minutes = 5;

		if (minutes < 1 || minutes > 10) {
			throw new FileException(EXPIRY_TIME_RANGE_INVALID);
		}

		Calendar expiryTime = Calendar.getInstance();
		expiryTime.add(Calendar.MINUTE, minutes);
		return expiryTime;
	}

	/**
	 * 루트 경로를 포함한 이미지 키를 생성한다.
	 * 확장자의 경우 .jpg로 고정한다.
	 *
	 * @param rootPath 저장할 루트 경로
	 * @return 생성된 이미지 키
	 */
	default String getImageKey(String rootPath) {
		if (rootPath.startsWith(PATH_DELIMITER)) {
			rootPath = rootPath.substring(1);
		}
		if (rootPath.endsWith(PATH_DELIMITER)) {
			rootPath = rootPath.substring(0, rootPath.length() - 1);
		}
		String uploadAt = LocalDate.now().format(ofPattern("yyyyMMdd"));
		String imageId = UUID.randomUUID() + "." + EXTENSION;
		return rootPath + PATH_DELIMITER + uploadAt + PATH_DELIMITER + imageId;
	}
}
