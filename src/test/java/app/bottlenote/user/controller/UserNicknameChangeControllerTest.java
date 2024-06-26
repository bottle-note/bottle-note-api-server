package app.bottlenote.user.controller;

import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.user.dto.request.NicknameChangeRequest;
import app.bottlenote.user.dto.response.NicknameChangeResponse;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.service.UserCommandService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserCommandController.class)
@WithMockUser
class UserNicknameChangeControllerTest {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private ObjectMapper mapper;
	@MockBean
	private UserCommandService nicknameChangeService;

	private MockedStatic<SecurityContextUtil> mockedSecurityUtil;

	@BeforeEach
	void setup() {
		mockedSecurityUtil = mockStatic(SecurityContextUtil.class);
		when(SecurityContextUtil.getCurrentUserId()).thenReturn(1L);
	}

	@AfterEach
	void tearDown() {
		mockedSecurityUtil.close();
	}

	@DisplayName("닉네임을 성공적으로 변경할 수 있다.")
	//@Test
	void shouldChangeNicknameSuccessfully() throws Exception {

		Long userId = 1L;

		NicknameChangeRequest request = new NicknameChangeRequest("newNickname");
		NicknameChangeResponse response = NicknameChangeResponse.builder()
			.message(NicknameChangeResponse.Message.SUCCESS)
			.userId(userId)
			.beforeNickname("beforeNickname")
			.changedNickname("newNickname")
			.build();

		when(nicknameChangeService.nicknameChange(userId,request)).thenReturn(response);

		mockMvc.perform(patch("/api/v1/users/nickname")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(request))
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value("true"))
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.data.message").value(response.getMessage()))
			.andExpect(jsonPath("$.data.userId").value(response.getUserId()))
			.andExpect(jsonPath("$.data.beforeNickname").value(response.getBeforeNickname()))
			.andExpect(jsonPath("$.data.changedNickname").value(response.getChangedNickname()))
			.andDo(print());
	}


	@DisplayName("닉네임 변경은 변경닉네임이 없으면 변경할 수 없다.")
	//@Test
	void shouldFailWhenEmptyParameter() throws Exception {
		NicknameChangeRequest request = new NicknameChangeRequest( "");

		mockMvc.perform(patch("/api/v1/users/nickname")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(request))
				.with(csrf()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value("false"))
			.andExpect(jsonPath("$.code").value("400"))
			.andExpect(jsonPath("$.errors.nickName").value("필드 'nickName'의 값 ''가 유효하지 않습니다: 닉네임은 2~11자의 한글, 영문, 숫자만 가능합니다."))
			.andDo(print());

	}

	@DisplayName("특수문자가 포함된 닉네임은 변경할 수 없다.")
	//@Test
	void shouldFailWhenInvalidNickname() throws Exception {
		NicknameChangeRequest request = new NicknameChangeRequest("#$%#$%#ㅁㅁ");

		mockMvc.perform(patch("/api/v1/users/nickname")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(request))
				.with(csrf()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value("false"))
			.andExpect(jsonPath("$.code").value("400"))
			.andExpect(jsonPath("$.errors.nickName").value("필드 'nickName'의 값 '#$%#$%#ㅁㅁ'가 유효하지 않습니다: 닉네임은 2~11자의 한글, 영문, 숫자만 가능합니다."))
			.andDo(print());
	}


	@DisplayName("중복된 닉네임은 변경할 수 없다.")
	//@Test
	void shouldFailWhenDuplicateNickname() throws Exception {
		Long userId = 1L;
		NicknameChangeRequest request = new NicknameChangeRequest("newNickname");

		when(nicknameChangeService.nicknameChange(userId, request)).thenThrow(new UserException(UserExceptionCode.USER_NICKNAME_NOT_VALID));

		mockMvc.perform(patch("/api/v1/users/nickname")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(request))
				.with(csrf()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value("false"))
			.andExpect(jsonPath("$.code").value("400"))
			.andExpect(jsonPath("$.errors.message").value("중복된 닉네임입니다."));
	}
}
