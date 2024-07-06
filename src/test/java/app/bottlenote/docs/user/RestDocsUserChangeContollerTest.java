package app.bottlenote.docs.user;

import app.bottlenote.docs.AbstractRestDocs;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.user.controller.UserCommandController;
import app.bottlenote.user.dto.request.NicknameChangeRequest;
import app.bottlenote.user.dto.response.NicknameChangeResponse;
import app.bottlenote.user.dto.response.ProfileImageChangeResponse;
import app.bottlenote.user.service.UserCommandService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static app.bottlenote.user.dto.response.NicknameChangeResponse.Message.SUCCESS;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("닉네임 변경 RestDocs용 테스트")
class RestDocsUserChangeContollerTest extends AbstractRestDocs {

	private final UserCommandService userCommandService = mock(UserCommandService.class);
	private MockedStatic<SecurityContextUtil> mockedSecurityUtil;

	@Override
	protected Object initController() {
		return new UserCommandController(userCommandService);
	}

	@BeforeEach
	void setup() {
		mockedSecurityUtil = mockStatic(SecurityContextUtil.class);
		mockedSecurityUtil.when(SecurityContextUtil::getUserIdByContext).thenReturn(Optional.of(1L));
	}


	@AfterEach
	void tearDown() {
		mockedSecurityUtil.close();
	}

	@Test
	@DisplayName("닉네임 변경을 할 수 있다.")
	void docs_1() throws Exception {

		Long userId = 1L;

		//given
		NicknameChangeRequest request = new NicknameChangeRequest("newNickname");
		NicknameChangeResponse response = NicknameChangeResponse.builder()
			.message(SUCCESS)
			.userId(userId)
			.beforeNickname("beforeNickname")
			.changedNickname("newNickname")
			.build();

		// when
		when(userCommandService.nicknameChange(userId, request)).thenReturn(response);

		//then
		mockMvc.perform(patch("/api/v1/users/nickname")
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf())
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())

			.andDo(document("user/nickname-change",
					requestFields(
						fieldWithPath("nickName").type(JsonFieldType.STRING).description("변경할 새 닉네임")
					),
					responseFields(
						fieldWithPath("success").description("응답 성공 여부"),
						fieldWithPath("code").description("응답 코드(http status code)"),
						fieldWithPath("data.message").description("메시지"),
						fieldWithPath("data.userId").description("사용자 ID"),
						fieldWithPath("data.beforeNickname").description("이전 닉네임"),
						fieldWithPath("data.changedNickname").description("변경된 닉네임"),
						fieldWithPath("errors").description("응답 성공 여부가 false일 경우 에러 메시지(없을 경우 null)"),
						fieldWithPath("meta.serverEncoding").ignored(),
						fieldWithPath("meta.serverVersion").ignored(),
						fieldWithPath("meta.serverPathVersion").ignored(),
						fieldWithPath("meta.serverResponseTime").ignored()
					)
				)
			);
	}

	@Test
	@DisplayName("프로필 이미지를 변경할 수 있다.")
	void docs_2() throws Exception {

		// given
		Long userId = 1L;
		String viewUrl = "http://example.com/new-profile-image.jpg";

		ProfileImageChangeResponse response = ProfileImageChangeResponse.builder()
			.userId(userId)
			.profileImageUrl(viewUrl)
			.callback("https://bottle-note.com/api/v1/users/" + userId)
			.build();

		// when
		when(userCommandService.profileImageChange(anyLong(), anyString())).thenReturn(response);

		Map<String, String> requestBody = new HashMap<>();
		requestBody.put("viewUrl", viewUrl);

		// then
		mockMvc.perform(patch("/api/v1/users/profile-image")
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf())
				.content(objectMapper.writeValueAsString(requestBody))) // JSON 문자열로 viewUrl 전달
			.andExpect(status().isOk())
			.andDo(document("user/profile-image-change",
				requestFields(
					fieldWithPath("viewUrl").description("변경할 프로필 이미지 URL")
				),
				responseFields(
					fieldWithPath("success").description("응답 성공 여부"),
					fieldWithPath("code").description("응답 코드(http status code)"),
					fieldWithPath("data.userId").description("사용자 ID"),
					fieldWithPath("data.profileImageUrl").description("변경된 프로필 이미지 URL"),
					fieldWithPath("data.callback").description("콜백 URL"),
					fieldWithPath("errors").description("응답 성공 여부가 false일 경우 에러 메시지(없을 경우 null)"),
					fieldWithPath("meta.serverEncoding").ignored(),
					fieldWithPath("meta.serverVersion").ignored(),
					fieldWithPath("meta.serverPathVersion").ignored(),
					fieldWithPath("meta.serverResponseTime").ignored()
				)
			));
	}

}
