package app.bottlenote.user.controller;

import app.bottlenote.global.data.response.Error;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.service.UserBasicService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static app.bottlenote.global.security.SecurityContextUtil.getUserIdByContext;
import static app.bottlenote.user.dto.response.WithdrawUserResultResponse.response;
import static app.bottlenote.user.dto.response.constant.WithdrawUserResultMessage.USER_WITHDRAW_SUCCESS;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("unit")
@DisplayName("[unit] [controller] UserBasicController")
@WebMvcTest(UserBasicController.class)
@WithMockUser
class UserBasicControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private UserBasicService userCommandService;

	private MockedStatic<SecurityContextUtil> mockedSecurityUtil;

	@BeforeEach
	void setup() {
		mockedSecurityUtil = mockStatic(SecurityContextUtil.class);
	}

	@AfterEach
	void tearDown() {
		mockedSecurityUtil.close();
	}

	@DisplayName("회원탈퇴에 성공한다.")
	@Test
	void testWithdrawUserSuccess() throws Exception {

		// when
		when(getUserIdByContext()).thenReturn(Optional.of(1L));

		when(userCommandService.withdrawUser(anyLong()))
			.thenReturn(response(USER_WITHDRAW_SUCCESS, 1L));

		mockMvc.perform(delete("/api/v1/users")
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf()))
			.andExpect(status().isOk())
			.andDo(print())
			.andExpect(jsonPath("$.success").value("true"))
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.data.message").value(USER_WITHDRAW_SUCCESS.getMessage()));
		// then
	}

	@DisplayName("존재하지 않는 회원은 회원탈퇴에 실패한다.")
	@Test
	void testWithdrawUserFailedWhenUserNotExist() throws Exception {

		Error error = Error.of(UserExceptionCode.REQUIRED_USER_ID);

		// when
		when(getUserIdByContext()).thenReturn(Optional.empty());

		when(userCommandService.withdrawUser(anyLong()))
			.thenReturn(response(USER_WITHDRAW_SUCCESS, 1L));

		// then
		mockMvc.perform(delete("/api/v1/users")
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf()))
			.andExpect(status().isBadRequest())
			.andDo(print())
			.andExpect(jsonPath("$.errors[0].code").value(String.valueOf(error.code())))
			.andExpect(jsonPath("$.errors[0].status").value(error.status().name()))
			.andExpect(jsonPath("$.errors[0].message").value(error.message()));
	}
}
