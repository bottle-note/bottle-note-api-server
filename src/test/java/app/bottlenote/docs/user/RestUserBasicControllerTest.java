package app.bottlenote.docs.user;

import app.bottlenote.docs.AbstractRestDocs;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.user.controller.UserBasicController;
import app.bottlenote.user.dto.response.UserProfileInfo;
import app.bottlenote.user.service.DefaultUserFacade;
import app.bottlenote.user.service.UserBasicService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Optional;

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

@DisplayName("유저 Command 컨트롤러 RestDocs용 테스트")
class RestUserBasicControllerTest extends AbstractRestDocs {

	private final UserBasicService userCommandService = mock(UserBasicService.class);
	private final DefaultUserFacade userFacade = mock(DefaultUserFacade.class);
	private MockedStatic<SecurityContextUtil> mockedSecurityUtil;

	@Override
	protected Object initController() {
		return new UserBasicController(userCommandService,userFacade);
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

	@DisplayName("현재 로그인한 유저 정보를 조회할 수 있다.")
	@Test
	void test_get_current_user_info() throws Exception {
		Long userId = 1L;

		UserProfileInfo response = UserProfileInfo.create(userId, "로그인한_유저_닉네임", "없을수도 잇음");
		when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(1L));
		when(userFacade.getUserProfileInfo(userId)).thenReturn(response);

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/users/current")
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf()))
			.andExpect(status().isOk())
			.andDo(
				document("user/get-current-user-info",
					responseFields(
						fieldWithPath("success").description("응답 성공 여부"),
						fieldWithPath("code").description("응답 코드(http status code)"),

						fieldWithPath("data.id").description("현재 로그인한 유저 아이디"),
						fieldWithPath("data.nickname").description("현재 로그인한 유저 닉네임"),
						fieldWithPath("data.imageUrl").description("현재 로그인한 유저 프로필 이미지 URL"),

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
