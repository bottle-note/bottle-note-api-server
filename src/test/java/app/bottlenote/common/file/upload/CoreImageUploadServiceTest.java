package app.bottlenote.common.file.upload;

import app.bottlenote.common.file.dto.request.ImageUploadRequest;
import app.bottlenote.common.file.dto.response.ImageUploadInfo;
import app.bottlenote.common.file.dto.response.ImageUploadResponse;
import app.bottlenote.common.file.exception.FileException;
import app.bottlenote.common.file.service.ImageUploadService;
import app.bottlenote.common.file.upload.fixture.FakeAmazonS3;
import app.bottlenote.common.file.upload.fixture.FakeImageEventPublisher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import static java.time.format.DateTimeFormatter.ofPattern;
import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
@DisplayName("[unit] [service] [fake] CoreImageUpload")
class CoreImageUploadServiceTest {
	private final static String ImageBucketName = "테스트-버킷-이름";
	private final static String cloudFrontUrl = "https://testUrl.cloudfront.net";
	private final static String awsUrl = "https://" + ImageBucketName + ".s3.amazonaws.com/";
	private final static String uploadAt = LocalDate.of(2024, 5, 1).format(ofPattern("yyyyMMdd"));
	private final static String fakeUUID = "ddd8d2d8-7b0c-47e9-91d0-d21251f891e8";
	private static final Logger log = LogManager.getLogger(CoreImageUploadServiceTest.class);
	private ImageUploadService imageUploadService;

	@BeforeEach
	void setUp() {
		imageUploadService = new ImageUploadService(
			new FakeImageEventPublisher(),
			new FakeAmazonS3(),
			ImageBucketName,
			cloudFrontUrl
		) {
			@Override
			public String getImageKey(String rootPath, Long index) {
				if (rootPath.startsWith(PATH_DELIMITER)) {
					rootPath = rootPath.substring(1);
				}
				if (rootPath.endsWith(PATH_DELIMITER)) {
					rootPath = rootPath.substring(0, rootPath.length() - 1);
				}
				String imageId = index + KEY_DELIMITER + fakeUUID + "." + EXTENSION;
				return rootPath + PATH_DELIMITER + uploadAt + PATH_DELIMITER + imageId;
			}
		};
	}

	@Test
	@DisplayName("PreSignUrl을 생성할 수 있다.")
	void test_1() {
		String key = imageUploadService.getImageKey("review", 1L);
		String preSignUrl = imageUploadService.generatePreSignUrl(key);

		log.info("PreSignUrl: {}", preSignUrl);
		assertNotNull(preSignUrl);
		assertEquals(awsUrl + key, preSignUrl);
	}

	@Test
	@DisplayName("업로드용 인증 URL을 생성할 수 있다.")
	void test_2() {
		ImageUploadRequest request = new ImageUploadRequest("review", 2L);

		ImageUploadResponse preSignUrl = imageUploadService.getPreSignUrl(request);

		assertNotNull(preSignUrl);
		assertEquals(request.uploadSize(), preSignUrl.uploadSize());
		assertEquals(5, preSignUrl.expiryTime());

		for (Long index = 1L; index <= preSignUrl.imageUploadInfo().size(); index++) {
			String imageKey = imageUploadService.getImageKey(request.rootPath(), index);

			String uploadUrlFixture = imageUploadService.generatePreSignUrl(imageKey);
			String viewUrlFixture = imageUploadService.generateViewUrl(cloudFrontUrl, imageKey);

			ImageUploadInfo info = preSignUrl.imageUploadInfo().get((int) (index - 1));

			log.info("[{}] ImageUploadInfo: {}", index, info);
			Assertions.assertEquals(index, info.order());
			Assertions.assertEquals(uploadUrlFixture, info.uploadUrl());
			Assertions.assertEquals(viewUrlFixture, info.viewUrl());
		}
	}

	@Test
	@DisplayName("조회용 URL을 생성할 수 있다.")
	void test_3() {
		String imageKey = imageUploadService.getImageKey("review", 1L);
		String viewUrl = imageUploadService.generateViewUrl(cloudFrontUrl, imageKey);

		log.info("ViewUrl: {}", viewUrl);
		assertNotNull(viewUrl);
		assertEquals(cloudFrontUrl + "/" + imageKey, viewUrl);
	}

	@Test
	@DisplayName("이미지 루트 경로와 인덱스를 제공해 이미지 키를 생성할 수 있다.")
	void test_4() {
		String imageKey = imageUploadService.getImageKey("review", 1L);
		String expected = "review/20240501/1-ddd8d2d8-7b0c-47e9-91d0-d21251f891e8.jpg";

		log.info("ImageKey: {}", imageKey);
		assertNotNull(imageKey);
		assertEquals(expected, imageKey);
	}

	@Test
	@DisplayName("기본 만료 시간은 5분이다.")
	void test_5() {
		// given
		Calendar expectedExpiryTime = Calendar.getInstance();
		expectedExpiryTime.add(Calendar.MINUTE, 5);

		// when
		Calendar actualExpiryTime = imageUploadService.getUploadExpiryTime(null);

		// then
		log.info("ExpiryTime: {}", actualExpiryTime);
		long diffInMillis = Math.abs(expectedExpiryTime.getTimeInMillis() - actualExpiryTime.getTimeInMillis());
		assertTrue(diffInMillis < TimeUnit.SECONDS.toMillis(1), "The difference should be less than 1 second");
	}

	@Test
	@DisplayName("최대 만료 시간은 10분이다.")
	void test_6() {
		// given
		Calendar expectedExpiryTime = Calendar.getInstance();
		expectedExpiryTime.add(Calendar.MINUTE, 10);

		// when
		Calendar actualExpiryTime = imageUploadService.getUploadExpiryTime(10);

		// then
		long diffInMillis = Math.abs(expectedExpiryTime.getTimeInMillis() - actualExpiryTime.getTimeInMillis());
		assertTrue(diffInMillis < TimeUnit.SECONDS.toMillis(1), "The difference should be less than 1 second");
		assertThrows(FileException.class, () -> imageUploadService.getUploadExpiryTime(11));
	}
}
