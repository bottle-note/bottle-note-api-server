package app.bottlenote.user.controller;


import static app.bottlenote.user.dto.response.NicknameChangeResponse.Message.SUCCESS;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import app.bottlenote.user.dto.request.NicknameChangeRequest;
import app.bottlenote.user.dto.response.NicknameChangeResponse;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.service.UserCommandService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
@WebMvcTest(UserCommandController.class)
@WithMockUser
class UserNicknameChangeControllerTest {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private ObjectMapper mapper;
	@MockBean
	private UserCommandService nicknameChangeService;


	@DisplayName("닉네임을 변경할수 있다.")
	@Test
	void shouldChangeNicknameSuccessfully() throws Exception {
		NicknameChangeRequest request = new NicknameChangeRequest(1L, "newNickname");
		NicknameChangeResponse response = NicknameChangeResponse.builder()
			.message(SUCCESS)
			.userId(1L)
			.beforeNickname("beforeNickname")
			.changedNickname("newNickname")
			.build();
		when(nicknameChangeService.nicknameChange(request)).thenReturn(response);

		mockMvc.perform(patch("/api/v1/users/nickname")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(request))
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value("true"))
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.data.message").value("닉네임이 성공적으로 변경되었습니다."))
			.andExpect(jsonPath("$.data.userId").value(1))
			.andExpect(jsonPath("$.data.beforeNickname").value("beforeNickname"))
			.andExpect(jsonPath("$.data.changedNickname").value("newNickname"));
	}

	@DisplayName("특수문자가 포함된 닉네임은 변경할 수 없다.")
	@Test
	void shouldFailWhenInvalidNickname() throws Exception {
		NicknameChangeRequest request = new NicknameChangeRequest(1L, "#$%#$%#ㅁㅁ");

		mockMvc.perform(patch("/api/v1/users/nickname")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(request))
				.with(csrf()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value("false"))
			.andExpect(jsonPath("$.code").value("400"))
			.andExpect(jsonPath("$.errors.nickName").value("필드 'nickName'의 값 '#$%#$%#ㅁㅁ'가 유효하지 않습니다: 닉네임은 2~11자의 한글, 영문, 숫자만 가능합니다."));
	}

	@DisplayName("닉네임변경은 필수 파라미터없으면 변경할 수 없다.")
	@Test
	void shouldFailWhenEmptyParameter() throws Exception {
		NicknameChangeRequest request = new NicknameChangeRequest(null, null);

		mockMvc.perform(patch("/api/v1/users/nickname")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(request))
				.with(csrf()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value("false"))
			.andExpect(jsonPath("$.code").value("400"))
			.andExpect(jsonPath("$.errors.userId").value("필드 'userId'의 값 'null'가 유효하지 않습니다: 유저 아이디를 확인 할 수 없습니다."));

	}

	@DisplayName("중복된 닉네임은 변경할 수 없다.")
	@Test
	void shouldFailWhenDuplicateNickname() throws Exception {
		NicknameChangeRequest request = new NicknameChangeRequest(1L, "newNickname");

		when(nicknameChangeService.nicknameChange(request)).thenThrow(new UserException(UserExceptionCode.USER_ALREADY_EXISTS));

		mockMvc.perform(patch("/api/v1/users/nickname")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(request))
				.with(csrf()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value("false"))
			.andExpect(jsonPath("$.code").value("400"))
			.andExpect(jsonPath("$.errors.message").value("이미 존재하는 사용자입니다."));
	}
}
