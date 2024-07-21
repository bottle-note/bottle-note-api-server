package app.bottlenote.common.file.upload;

import app.bottlenote.common.file.upload.dto.request.ImageUploadRequest;
import app.bottlenote.common.file.upload.dto.response.ImageUploadInfo;
import app.bottlenote.common.file.upload.dto.response.ImageUploadResponse;
import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URL;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.when;

@Tag("unit")
@DisplayName("[Mock] 이미지 업로드 서비스 테스트")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ImageUploadServiceTest {

	private static final Logger log = LogManager.getLogger(ImageUploadServiceTest.class);
	@InjectMocks
	private ImageUploadService imageUploadService;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@Mock
	private AmazonS3 amazonS3;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(imageUploadService, "ImageBucketName", "image-bucket");
		ReflectionTestUtils.setField(imageUploadService, "cloudFrontUrl", "https://testUrl.cloudfront.net");
	}

	@Test
	@DisplayName("단건 이미지 업로드 URL을 생성할 수 있다.")
	void test_1() throws Exception {
		// Given
		String amazonUrl = "https://bottlenote.s3.ap-northeast-2.amazonaws.com/";
		String viewUrl = "https://testUrl.cloudfront.net/";
		String rootPath = "image-upload";
		Long uploadSize = 1L;
		String updateAt = "20240524";
		String imageName = "ddd8d2d8-7b0c-47e9-91d0-d21251f891e8.jpg";
		String key1 = rootPath + "/" + updateAt + "/" + "1-" + imageName;

		ImageUploadRequest 요청객체 = new ImageUploadRequest(rootPath, uploadSize); // 요청 사이즈를 2로 수정
		ImageUploadResponse 응답객체 = new ImageUploadResponse("image-bucket", 1, 5,
			List.of(new ImageUploadInfo(1L, viewUrl + key1, amazonUrl + key1)/*,new ImageUploadInfo(2L, viewUrl + key2, amazonUrl + key2)*/)
		);

		//when
		when(amazonS3.generatePresignedUrl(anyString(), anyString(), any(), any(HttpMethod.class)))
			.thenReturn(new URL(amazonUrl + key1));
		willDoNothing().given(eventPublisher).publishEvent(any());

		ImageUploadResponse 실제_반환값 = imageUploadService.getPreSignUrl(요청객체);

		// Then
		ImageUploadInfo 비교_대상 = 응답객체.imageUploadInfo().stream().findFirst().get();
		ImageUploadInfo 실제_비교_대상 = 실제_반환값.imageUploadInfo().stream().findFirst().get();

		Assertions.assertNotNull(실제_반환값);
		Assertions.assertEquals(응답객체.bucketName(), 실제_반환값.bucketName());
		Assertions.assertEquals(응답객체.expiryTime(), 실제_반환값.expiryTime());
		Assertions.assertEquals(비교_대상.order(), 실제_비교_대상.order());
		Assertions.assertEquals(비교_대상.uploadUrl(), 실제_비교_대상.uploadUrl());
		Assertions.assertTrue(실제_비교_대상.viewUrl().startsWith(viewUrl));
	}

	@Test
	@DisplayName("이미지 view URL을 생성할 수 있다.")
	void test_2() {
		String cloudFrontUrl = "https://testUrl.cloudfront.net";
		String imageKey = "1-test.jpg";
		String viewUrl = imageUploadService.generateViewUrl(cloudFrontUrl, imageKey);

		Assertions.assertEquals("https://testUrl.cloudfront.net/1-test.jpg", viewUrl);
	}

	@Test
	@DisplayName("업로드 이미지 경로를 생성할 수 있다.")
	void test_3() throws Exception {
		// Given
		String imageKey = imageUploadService.getImageKey("test", 1L);
		String amazonUrl = "https://bottlenote.s3.ap-northeast-2.amazonaws.com/";
		when(amazonS3.generatePresignedUrl(anyString(), anyString(), any(), any(HttpMethod.class)))
			.thenReturn(new URL(amazonUrl + imageKey));

		// when
		String preSignUrl = imageUploadService.generatePreSignUrl(imageKey);

		//then
		Assertions.assertEquals((amazonUrl + imageKey), preSignUrl);
	}
}
