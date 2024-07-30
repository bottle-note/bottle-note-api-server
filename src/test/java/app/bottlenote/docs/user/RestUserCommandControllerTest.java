package app.bottlenote.docs.user;

import static app.bottlenote.user.dto.response.WithdrawUserResultResponse.response;
import static app.bottlenote.user.dto.response.constant.WithdrawUserResultMessage.USER_WITHDRAW_SUCCESS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.docs.AbstractRestDocs;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.user.controller.UserCommandController;
import app.bottlenote.user.service.UserCommandService;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@DisplayName("유저 Command 컨트롤러 RestDocs용 테스트")
class RestUserCommandControllerTest extends AbstractRestDocs {

	private final UserCommandService userCommandService = mock(UserCommandService.class);
	private MockedStatic<SecurityContextUtil> mockedSecurityUtil;

	@Override
	protected Object initController() {
		return new UserCommandController(userCommandService);
	}

	@BeforeEach
	void setup() {
		mockedSecurityUtil = mockStatic(SecurityContextUtil.class);
	}


	@AfterEach
	void tearDown() {
		mockedSecurityUtil.close();
	}

	@DisplayName("회원탈퇴를 할 수 있다.")
	@Test
	void test_withdraw_success() throws Exception {
		Long userId = 1L;

		when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(1L));

		when(userCommandService.withdrawUser(userId))
			.thenReturn(response(USER_WITHDRAW_SUCCESS, userId));

		mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/users")
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf()))
			.andExpect(status().isOk())
			.andDo(
				document("user/withdraw-user",
					responseFields(
						fieldWithPath("success").description("응답 성공 여부"),
						fieldWithPath("code").description("응답 코드(http status code)"),
						fieldWithPath("data.codeMessage").description("성공 메시지 코드"),
						fieldWithPath("data.message").description("성공 메시지"),
						fieldWithPath("data.userId").description("유저 아이디"),
						fieldWithPath("data.responseAt").description("서버 응답 일시"),
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
