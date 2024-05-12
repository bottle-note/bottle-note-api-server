package app.bottlenote.user.controller;


import static app.bottlenote.user.dto.response.NicknameChangeResponse.NicknameChangeResponseEnum.SUCCESS;
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


	@DisplayName("닉네임 변경 성공 테스트")
	@Test
	void shouldChangeNicknameSuccessfully() throws Exception {
		NicknameChangeRequest request = new NicknameChangeRequest(1L, "newNickname");
		NicknameChangeResponse response = NicknameChangeResponse.of(SUCCESS, 1L, "beforeNickname", "newNickname");

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

	@DisplayName("유효하지 않은 닉네임으로 변경 시도 시 실패 테스트")
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

	@DisplayName("닉네임 변경 요청에 파라미터값이 없으면 실패")
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

	@DisplayName("닉네임 중복 변경 시도 시 실패 테스트")
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
