package app.bottlenote.common.file.service;


import app.bottlenote.common.annotation.ThirdPartyService;
import app.bottlenote.common.file.PreSignUrlProvider;
import app.bottlenote.common.file.dto.request.ImageUploadRequest;
import app.bottlenote.common.file.dto.response.ImageUploadItem;
import app.bottlenote.common.file.dto.response.ImageUploadResponse;
import app.bottlenote.common.file.event.payload.S3RequestEvent;
import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Slf4j
@ThirdPartyService
public class ImageUploadService implements PreSignUrlProvider {

	private static final Integer EXPIRY_TIME = 5;
	private final ApplicationEventPublisher eventPublisher;
	private final AmazonS3 amazonS3;
	private final String imageBucketName;
	private final String cloudFrontUrl;

	public ImageUploadService(
		ApplicationEventPublisher eventPublisher,
		AmazonS3 amazonS3,
		@Value("${amazon.aws.bucket}")
		String imageBucketName,
		@Value("${amazon.aws.cloudFrontUrl}")
		String cloudFrontUrl
	) {
		this.eventPublisher = eventPublisher;
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
				ImageUploadItem.builder()
					.order(index)
					.viewUrl(viewUrl)
					.uploadUrl(preSignUrl)
					.build()
			);
		}
		eventPublisher.publishEvent(S3RequestEvent.of("s3 Image upload", imageBucketName, uploadSize));

		return ImageUploadResponse.builder()
			.bucketName(imageBucketName)
			.expiryTime(EXPIRY_TIME)
			.uploadSize(keys.size())
			.imageUploadInfo(keys)
			.build();
	}

	@Override
	public String generateViewUrl(String cloudFrontUrl, String imageKey) {
		return cloudFrontUrl
			+ PATH_DELIMITER
			+ imageKey;
	}

	@Override
	public String generatePreSignUrl(String imageKey) {
		Calendar uploadExpiryTime = getUploadExpiryTime(EXPIRY_TIME);
		return amazonS3.generatePresignedUrl(
			imageBucketName,
			imageKey,
			uploadExpiryTime.getTime(),
			HttpMethod.PUT
		).toString();
	}
}
