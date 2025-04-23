package app.bottlenote.common.file.upload;

import app.bottlenote.common.file.controller.ImageUploadController;
import app.bottlenote.common.file.dto.request.ImageUploadRequest;
import app.bottlenote.common.file.dto.response.ImageUploadItem;
import app.bottlenote.common.file.dto.response.ImageUploadResponse;
import app.bottlenote.common.file.service.ImageUploadService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.stream.Stream;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("unit")
@ActiveProfiles("test")
@DisplayName("[unit] [controller] [mock] CoreImageUpload")
@WebMvcTest(controllers = {ImageUploadController.class})
class ImageUploadControllerTest {
	@Autowired
	protected ObjectMapper mapper;
	@Autowired
	protected MockMvc mockMvc;
	@MockBean
	private ImageUploadService imageUploadService;

	static Stream<Arguments> provider_1() {
		return Stream.of(
			Arguments.of("모든값이 있을때", new ImageUploadRequest("images", 2L)),
			Arguments.of("사이즈가 없을때", new ImageUploadRequest("images", null))
		);
	}

	@WithMockUser()
	@DisplayName("인증된 이미지 업로드 경로 요청 할 수 있다.")
	@ParameterizedTest(name = "{0}")
	@MethodSource("provider_1")
	void test_1(String description, ImageUploadRequest request) throws Exception {
		System.out.println("test -" + description);
		//given
		Long size = request.uploadSize();
		List<ImageUploadItem> infos = List.of(
			ImageUploadItem.builder().order(1L)
				.uploadUrl("https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/1")
				.viewUrl("https://d1d1d1d1.cloudfront.net/images/1")
				.build(),
			ImageUploadItem.builder().order(2L)
				.uploadUrl("https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/2")
				.viewUrl("https://d1d1d1d1.cloudfront.net/images/2")
				.build()
		);
		ImageUploadResponse response = ImageUploadResponse.builder()
			.imageUploadInfo(infos)
			.uploadSize(size.intValue())
			.bucketName("image-bucket")
			.expiryTime(5)
			.build();

		//when
		given(imageUploadService.getPreSignUrl(request)).willReturn(response);

		//then
		ResultActions resultActions = mockMvc.perform(get("/api/v1/s3/presign-url")
				.param("rootPath", request.rootPath())
				.param("uploadSize", String.valueOf(request.uploadSize()))
				.with(csrf())
			)
			.andExpect(status().isOk())
			.andDo(print());

		resultActions.andExpect(jsonPath("$.success").value("true"));
		resultActions.andExpect(jsonPath("$.code").value("200"));
		resultActions.andExpect(jsonPath("$.data.uploadSize").value(size));
		resultActions.andExpect(jsonPath("$.data.imageUploadInfo[0].order").value(1));
		resultActions.andExpect(jsonPath("$.data.imageUploadInfo[0].uploadUrl").value("https://bottlenote.s3.ap-northeast-2.amazonaws.com/images/1"));
		resultActions.andExpect(jsonPath("$.data.imageUploadInfo[0].viewUrl").value("https://d1d1d1d1.cloudfront.net/images/1"));
	}

	@Test
	@WithAnonymousUser
	@DisplayName("인증되지 않은 유저가 요청할 경우 예외가 발생한다.")
	void test_2() throws Exception {
		//given
		ImageUploadRequest request = new ImageUploadRequest("images", 2L);
		//then
		mockMvc.perform(get("/api/v1/s3/presign-url")
				.param("rootPath", request.rootPath())
				.param("uploadSize", String.valueOf(request.uploadSize()))
			)
			.andExpect(status().isUnauthorized())
			.andDo(print());
	}
}
