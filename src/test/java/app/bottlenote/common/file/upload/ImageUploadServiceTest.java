package app.bottlenote.common.file.upload;

import app.bottlenote.common.file.upload.dto.request.ImageUploadRequest;
import app.bottlenote.common.file.upload.dto.response.ImageUploadInfo;
import app.bottlenote.common.file.upload.dto.response.ImageUploadResponse;
import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@DisplayName("이미지 업로드 서비스 테스트")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ImageUploadServiceTest {

	@InjectMocks
	private ImageUploadService imageUploadService;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@Mock
	private AmazonS3 amazonS3;

	private ImageUploadRequest request;
	private ImageUploadResponse response;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(imageUploadService, "ImageBucketName", "image-bucket");
		ReflectionTestUtils.setField(imageUploadService, "cloudFrontUrl", "https://testUrl.cloudfront.net");

		request = new ImageUploadRequest("test", 2L); // 요청 사이즈를 2로 수정

		List<ImageUploadInfo> infos = List.of(
			new ImageUploadInfo(1L, "https://testUrl.cloudfront.net/1-test/20240524/test.jpg", "https://bottlenote.s3.ap-northeast-2.amazonaws.com/1-test/20240524/test.jpg"),
			new ImageUploadInfo(2L, "https://testUrl.cloudfront.net/2-test/20240524/test.jpg", "https://bottlenote.s3.ap-northeast-2.amazonaws.com/2-test/20240524/test.jpg")
		);

		response = new ImageUploadResponse("image-bucket", 2, 5, infos);
	}

	@Test
	@DisplayName("이미지 업로드 URL을 생성할 수 있다.")
	void test_1() {
		// Given
		given(amazonS3.generatePresignedUrl(anyString(), anyString(), any(), any(HttpMethod.class)))
			.willAnswer(invocation -> {
				String key = invocation.getArgument(1);
				return new URL("https://bottlenote.s3.ap-northeast-2.amazonaws.com/" + key);
			});

		willDoNothing().given(eventPublisher).publishEvent(any());

		// ImageUploadService를 스파이로 만듭니다.
		ImageUploadService spyImageUploadService = spy(imageUploadService);
		when(spyImageUploadService.getImageKey(anyString())).thenReturn("test/20240524/test.jpg");

		// When
		ImageUploadResponse actualResponse = spyImageUploadService.getPreSignUrl(request);

		// Then
		Assertions.assertNotNull(actualResponse);
		Assertions.assertEquals(response.bucketName(), actualResponse.bucketName());
		Assertions.assertEquals(response.expiryTime(), actualResponse.expiryTime());

		for (int i = 0; i < response.imageUploadInfo().size(); i++) {
			ImageUploadInfo expectedInfo = response.imageUploadInfo().get(i);
			ImageUploadInfo actualInfo = actualResponse.imageUploadInfo().get(i);
			Assertions.assertEquals(expectedInfo.order(), actualInfo.order());
			Assertions.assertEquals(expectedInfo.uploadUrl(), actualInfo.uploadUrl());
			Assertions.assertEquals(expectedInfo.viewUrl(), actualInfo.viewUrl());
		}
	}

	@Test
	@DisplayName("이미지 view URL을 생성할 수 있다.")
	void test_2() {
		String cloudFrontUrl = "https://testUrl.cloudfront.net";
		String imageKey = "test.jpg";
		Long index = 1L;

		String viewUrl = imageUploadService.generateViewUrl(cloudFrontUrl, imageKey, index);

		Assertions.assertEquals("https://testUrl.cloudfront.net/1-test.jpg", viewUrl);
	}

	@Test
	@DisplayName("이미지 업로드 만료 시간을 계산할 수 있다.")
	public void test_3() throws Exception {
		String imageKey = "test.jpg";
		Long index = 1L;

		// Correct mocking setup
		URL mockUrl = new URL("https://bottlenote.s3.ap-northeast-2.amazonaws.com/test.jpg");
		when(amazonS3.generatePresignedUrl(anyString(), anyString(), any(), any(HttpMethod.class)))
			.thenReturn(mockUrl);

		String preSignUrl = imageUploadService.generatePreSignUrl(imageKey, index);

		Assertions.assertEquals(mockUrl.toString(), preSignUrl);
	}

}
