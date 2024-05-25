package app.bottlenote.common.file.upload.fakes;

import app.bottlenote.common.file.PreSignUrlProvider;
import app.bottlenote.common.file.upload.ImageUploadService;
import app.bottlenote.common.file.upload.dto.request.ImageUploadRequest;
import app.bottlenote.common.file.upload.dto.response.ImageUploadInfo;
import app.bottlenote.common.file.upload.dto.response.ImageUploadResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static app.bottlenote.common.file.PreSignUrlProvider.PATH_DELIMITER;
import static java.time.format.DateTimeFormatter.ofPattern;
import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("unit")
@Tag("fake")
class FakeImageUploadServiceTest {
	private final static String ImageBucketName = "테스트-버킷-이름";
	private final static String cloudFrontUrl = "https://testUrl.cloudfront.net";
	private final static String awsUrl = "https://" + ImageBucketName + ".s3.amazonaws.com/";
	private final static String uploadAt = LocalDate.of(2024, 5, 1).format(ofPattern("yyyyMMdd"));
	private final static String fakeUUID = "ddd8d2d8-7b0c-47e9-91d0-d21251f891e8";
	private ImageUploadService imageUploadService;
	private ImageUploadRequest request;

	@BeforeEach
	void setUp() {
		imageUploadService = new ImageUploadService(
			new FakeEventPublisher(),
			new FakeAmazonS3(),
			ImageBucketName,
			cloudFrontUrl
		) {
			@Override
			public String getImageKey(String rootPath) {
				if (rootPath.startsWith(PATH_DELIMITER)) {
					rootPath = rootPath.substring(1);
				}
				if (rootPath.endsWith(PATH_DELIMITER)) {
					rootPath = rootPath.substring(0, rootPath.length() - 1);
				}
				return rootPath + PATH_DELIMITER + uploadAt + PATH_DELIMITER + fakeUUID + "." + EXTENSION;
			}
		};


		request = new ImageUploadRequest("fake-test", 2L);
	}

	@Test
	@DisplayName("업로드용 인증 URL을 생성할 수 있다.	")
	void getPreSignUrl() {

		ImageUploadResponse preSignUrl = imageUploadService.getPreSignUrl(request);

		assertNotNull(preSignUrl);
		assertEquals(request.uploadSize(), preSignUrl.uploadSize());
		assertEquals(5, preSignUrl.expiryTime());


		for (int index = 1; index <= preSignUrl.imageUploadInfo().size(); index++) {
			String uploadUrlFixture = uploadUrlFixture(index);
			String viewUrlFixture = viewUrlFixture(index);
			ImageUploadInfo info = preSignUrl.imageUploadInfo().get(index - 1);

			Assertions.assertEquals(index, info.order());
			Assertions.assertEquals(uploadUrlFixture, info.uploadUrl());
			Assertions.assertEquals(viewUrlFixture, info.viewUrl());
		}
	}

	private String uploadUrlFixture(int index) {
		return awsUrl + index + "-" + request.rootPath() + PATH_DELIMITER + uploadAt + PATH_DELIMITER + fakeUUID + "." + PreSignUrlProvider.EXTENSION;
	}

	private String viewUrlFixture(int index) {
		return cloudFrontUrl + PATH_DELIMITER + index + "-" + request.rootPath() + PATH_DELIMITER + uploadAt + PATH_DELIMITER + fakeUUID + "." + PreSignUrlProvider.EXTENSION;
	}

}
