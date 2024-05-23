package app.bottlenote.common.file.upload;


import app.bottlenote.common.file.PreSignUrlProvider;
import app.bottlenote.common.file.upload.dto.request.ImageUploadRequest;
import app.bottlenote.common.file.upload.dto.response.ImageUploadInfo;
import app.bottlenote.common.file.upload.dto.response.ImageUploadResponse;
import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class ImageUploadService implements PreSignUrlProvider {

	private final static Integer EXPIRY_TIME = 5;
	private final AmazonS3 amazonS3;
	@Value("${amazon.aws.bucket}")
	private String ImageBucketName;
	@Value("${amazon.aws.cloudFrontUrl}")
	private String cloudFrontUrl;

	/**
	 * 업로드용 인증 URL을 생성한다.
	 *
	 * @param request 업로드랑 루트 경로 + 업로드할 사이즈
	 * @return the 생성된 이미지 업로드 정보
	 */
	public ImageUploadResponse getPreSignUrl(ImageUploadRequest request) {
		String rootPath = request.rootPath();
		Long uploadSize = request.uploadSize();
		String imageKey = getImageKey(rootPath);
		List<ImageUploadInfo> keys = new ArrayList<>();

		for (long index = 1; index <= uploadSize; index++) {
			String preSignUrl = generatePreSignUrl(imageKey, index);
			String viewUrl = generateViewUrl(cloudFrontUrl, imageKey, index);

			keys.add(
				ImageUploadInfo.builder()
					.order(index)
					.viewUrl(viewUrl)
					.uploadUrl(preSignUrl)
					.build()
			);
		}

		return ImageUploadResponse.builder()
			.bucketName(ImageBucketName)
			.expiryTime(EXPIRY_TIME)
			.uploadSize(keys.size())
			.imageUploadInfo(keys)
			.build();
	}

	@Override
	public String generateViewUrl(String cloudFrontUrl, String imageKey, Long index) {
		return cloudFrontUrl
			+ PATH_DELIMITER
			+ index
			+ KEY_DELIMITER
			+ imageKey;
	}

	@Override
	public String generatePreSignUrl(String imageKey, Long index) {
		Calendar uploadExpiryTime = getUploadExpiryTime(EXPIRY_TIME);
		return amazonS3
			.generatePresignedUrl(
				ImageBucketName,
				index + KEY_DELIMITER + imageKey,
				uploadExpiryTime.getTime(),
				HttpMethod.PUT
			).toString();
	}
}
