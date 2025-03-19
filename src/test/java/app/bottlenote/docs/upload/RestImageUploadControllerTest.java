package app.bottlenote.docs.upload;

import app.bottlenote.common.file.controller.ImageUploadController;
import app.bottlenote.common.file.dto.request.ImageUploadRequest;
import app.bottlenote.common.file.dto.response.ImageUploadInfo;
import app.bottlenote.common.file.dto.response.ImageUploadResponse;
import app.bottlenote.common.file.service.ImageUploadService;
import app.bottlenote.docs.AbstractRestDocs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("이미지 업로드 RestDocs용 테스트")
class RestImageUploadControllerTest extends AbstractRestDocs {

	private final ImageUploadService imageUploadService = mock(ImageUploadService.class);

	@Override
	protected Object initController() {
		return new ImageUploadController(imageUploadService);
	}

	@WithMockUser()
	@DisplayName("인증된 이미지 업로드 경로 요청 할 수 있다.")
	@Test
	void test_1() throws Exception {
		//given
		ImageUploadRequest request = new ImageUploadRequest("images", 1L);
		ImageUploadResponse response = ImageUploadResponse.builder()
			.imageUploadInfo(
				List.of(ImageUploadInfo.builder().order(1L)
					.uploadUrl("https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/1")
					.viewUrl("https://d1d1d1d1.cloudfront.net/images/1")
					.build()
				))
			.uploadSize(request.uploadSize().intValue())
			.bucketName("image-bucket")
			.expiryTime(5)
			.build();

		//when
		given(imageUploadService.getPreSignUrl(request)).willReturn(response);

		//then
		mockMvc.perform(get("/api/v1/s3/presign-url")
				.param("rootPath", request.rootPath())
				.param("uploadSize", String.valueOf(request.uploadSize()))
			)
			.andExpect(status().isOk())
			.andDo(
				document("file/image/upload/presign-url",
					queryParameters(
						parameterWithName("rootPath").description("업로드 파일 경로 (하단 설명 참조)"),
						parameterWithName("uploadSize").description("업로드할 이미지의 사이즈 ( 이미지당 1개 )")
					),
					responseFields(
						fieldWithPath("success").description("응답 성공 여부"),
						fieldWithPath("code").description("응답 코드(http status code)"),
						fieldWithPath("data.bucketName").description("버킷 이름"),
						fieldWithPath("data.uploadSize").description("업로드 파일 사이즈"),
						fieldWithPath("data.expiryTime").description("업로드 URL 만료 시간(분단위)"),
						fieldWithPath("data.imageUploadInfo[].order").description("이미지 업로드 순서"),
						fieldWithPath("data.imageUploadInfo[].viewUrl").description("이미지 조회 URL"),
						fieldWithPath("data.imageUploadInfo[].uploadUrl").description("이미지 업로드 URL"),
						fieldWithPath("errors").ignored(),
						fieldWithPath("meta.serverEncoding").ignored(),
						fieldWithPath("meta.serverVersion").ignored(),
						fieldWithPath("meta.serverPathVersion").ignored(),
						fieldWithPath("meta.serverResponseTime").ignored()
					)
				)
			);
	}

}
