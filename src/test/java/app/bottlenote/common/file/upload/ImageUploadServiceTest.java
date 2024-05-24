package app.bottlenote.common.file.upload;

import app.bottlenote.common.file.upload.dto.request.ImageUploadRequest;
import app.bottlenote.common.file.upload.dto.response.ImageUploadInfo;
import app.bottlenote.common.file.upload.dto.response.ImageUploadResponse;
import com.amazonaws.services.s3.AmazonS3;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;

@DisplayName("이미지 업로드 서비스 테스트")
@ExtendWith(MockitoExtension.class)
class ImageUploadServiceTest {
	ImageUploadRequest request;
	ImageUploadResponse response;
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

		request = new ImageUploadRequest("test", 1L);
		List<ImageUploadInfo> infos = List.of(
			ImageUploadInfo.builder().order(1L)
				.uploadUrl("https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/1")
				.viewUrl("https://testUrl.cloudfront.net/images/1")
				.build(),
			ImageUploadInfo.builder().order(2L)
				.uploadUrl("https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/2")
				.viewUrl("https://testUrl.cloudfront.net/images/2")
				.build()
		);

		response = ImageUploadResponse.builder()
			.imageUploadInfo(infos)
			.uploadSize(request.uploadSize().intValue())
			.bucketName("image-bucket")
			.expiryTime(5)
			.build();

	}

	@Test
	@DisplayName("이미지 업로드 URL을 생성한다.")
	void getPreSignUrl() throws MalformedURLException {
		// given
		String uploadUrl = response.imageUploadInfo().get(0).uploadUrl();
		String viewUrl = response.imageUploadInfo().get(0).viewUrl();
		URL url = new URL(uploadUrl);

		// when
		given(amazonS3.generatePresignedUrl(any(), any(), any(), any())).willReturn(url);
		given(imageUploadService.generateViewUrl(anyString(), anyString(),anyLong())).willReturn(viewUrl);
		willDoNothing().given(eventPublisher).publishEvent(any());

		ImageUploadResponse preSignUrl = imageUploadService.getPreSignUrl(request);

		// then
		Assertions.assertEquals(response, preSignUrl);
	}

}
