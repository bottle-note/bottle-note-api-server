package app.bottlenote.docs.user;

import app.bottlenote.docs.AbstractRestDocs;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.user.controller.UserCommandController;
import app.bottlenote.user.dto.request.NicknameChangeRequest;
import app.bottlenote.user.dto.response.NicknameChangeResponse;
import app.bottlenote.user.service.UserCommandService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import static app.bottlenote.user.dto.response.NicknameChangeResponse.Message.SUCCESS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
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
		when(SecurityContextUtil.getCurrentUserId()).thenReturn(1L);
	}

	@AfterEach
	void tearDown() {
		mockedSecurityUtil.close();
	}

	//@Test
	@DisplayName("닉네임 변경을 할 수 있다.")
	void changeNickname_test() throws Exception {


		//given
		NicknameChangeRequest request = new NicknameChangeRequest(1L, "newNickname");
		NicknameChangeResponse response = NicknameChangeResponse.builder()
			.message(SUCCESS)
			.userId(1L)
			.beforeNickname("beforeNickname")
			.changedNickname("newNickname")
			.build();

		// when
		when(userCommandService.nicknameChange(request)).thenReturn(response);

		//then
		mockMvc.perform(patch("/api/v1/users/nickname")
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf())
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())

			.andDo(document("user/nickname-change",
					requestFields(
						fieldWithPath("userId").type(JsonFieldType.NUMBER).description("사용자의 ID"),
						fieldWithPath("nickName").type(JsonFieldType.STRING).description("변경할 새 닉네임")
					),
					responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
						fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드(http status code)"),
						fieldWithPath("data.message").type(JsonFieldType.STRING).description("메시지"),
						fieldWithPath("data.userId").type(JsonFieldType.NUMBER).description("사용자 ID"),
						fieldWithPath("data.beforeNickname").type(JsonFieldType.STRING).description("이전 닉네임"),
						fieldWithPath("data.changedNickname").type(JsonFieldType.STRING).description("변경된 닉네임"),
						fieldWithPath("errors").type(JsonFieldType.ARRAY).description("응답 성공 여부가 false일 경우 에러 메시지(없을 경우 null)"),
						fieldWithPath("meta.serverEncoding").ignored(),
						fieldWithPath("meta.serverVersion").ignored(),
						fieldWithPath("meta.serverPathVersion").ignored(),
						fieldWithPath("meta.serverResponseTime").ignored()
					)
				)
			);
	}
}
