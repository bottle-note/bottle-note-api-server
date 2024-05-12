package app.bottlenote.docs.user;

import static app.bottlenote.user.dto.response.NicknameChangeResponse.NicknameChangeResponseEnum.SUCCESS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.docs.AbstractRestDocs;
import app.bottlenote.user.controller.UserCommandController;
import app.bottlenote.user.dto.request.NicknameChangeRequest;
import app.bottlenote.user.dto.response.NicknameChangeResponse;
import app.bottlenote.user.service.UserCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

@DisplayName("닉네임 변경 RestDocs용 테스트")
class RestDocsUserChangeContollerTest extends AbstractRestDocs {

	private final UserCommandService userCommandService = mock(UserCommandService.class);

	@Override
	protected Object initController() {
		return new UserCommandController(userCommandService);
	}

	@Test
	@DisplayName("닉네임 변경을 할 수 있다.")
	void changeNickname_test() throws Exception {
		//given
		NicknameChangeRequest request = new NicknameChangeRequest(1L, "newNickname");
		NicknameChangeResponse response = NicknameChangeResponse.of(SUCCESS, 1L, "oldNickname", "newNickname");

		//when
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
						fieldWithPath("meta.serverEncoding").description("서버 인코딩 정도"),
						fieldWithPath("meta.serverVersion").description("서버 버전"),
						fieldWithPath("meta.serverPathVersion").description("서버 경로 버전"),
						fieldWithPath("meta.serverResponseTime").description("서버 응답 시간")
					)
				)
			);
	}
}
