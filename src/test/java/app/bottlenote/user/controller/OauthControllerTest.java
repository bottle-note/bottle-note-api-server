package app.bottlenote.user.controller;

import app.bottlenote.user.domain.constant.GenderType;
import app.bottlenote.user.domain.constant.SocialType;
import app.bottlenote.user.dto.request.OauthRequest;
import app.bottlenote.user.dto.response.TokenDto;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.service.OauthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Tag("unit")
@DisplayName("[unit] [controller] OauthController")
@WebMvcTest(OauthController.class)
@WithMockUser
class OauthControllerTest {

	@Autowired
	protected ObjectMapper mapper;
	@Autowired
	protected MockMvc mockMvc;
	@MockBean
	protected OauthService oauthService;

	private TokenDto tokenDto;

	@BeforeEach
	void setUp() {
		tokenDto = TokenDto.builder()
			.accessToken("access-token")
			.refreshToken("refresh-token")
			.build();
	}


	@Test
	@DisplayName("유저는 로그인 할 수 있다.")
	void user_login_test() throws Exception {

		//given
		OauthRequest oauthRequest = new OauthRequest("cdm2883@naver.com", SocialType.KAKAO,
			GenderType.MALE,
			27);

		TokenDto tokenDto = TokenDto.builder()
			.accessToken("accessToken")
			.refreshToken("refreshToken")
			.build();

		//when
		when(oauthService.oauthLogin(oauthRequest)).thenReturn(tokenDto);

		//then
		mockMvc.perform(post("/api/v1/oauth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf())
				.content(mapper.writeValueAsString(oauthRequest)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.data.accessToken").value("accessToken"))
			.andExpect(cookie().value("refresh-token", "refreshToken"))
			.andExpect(cookie().httpOnly("refresh-token", true))
			.andExpect(cookie().secure("refresh-token", true))
			.andDo(print());
	}

	@Test
	@DisplayName("유저는 SocialType이 null 값이면 로그인 할 수 없다.")
	void user_login_fail_when_socialType_is_null() throws Exception {

		//given
		OauthRequest oauthRequest = new OauthRequest("cdm2883@naver.com", null,
			GenderType.MALE, 27);


		//when
		when(oauthService.oauthLogin(oauthRequest)).thenReturn(tokenDto);

		//then
		mockMvc.perform(post("/api/v1/oauth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf())
				.content(mapper.writeValueAsString(oauthRequest)))
			.andExpect(status().isBadRequest())
			.andExpect(result -> assertTrue(
				result.getResolvedException() instanceof MethodArgumentNotValidException))
			.andExpect(jsonPath("$.errors").exists());
	}

	@Test
	@DisplayName("유저나이가 유효하지 않은 값이면 로그인 할 수 없다.")
	void user_login_fail_when_gender_is_null() throws Exception {

		//given
		OauthRequest oauthRequest = new OauthRequest("cdm2883@naver.com", SocialType.KAKAO,
			GenderType.MALE, -10);

		//when
		when(oauthService.oauthLogin(oauthRequest)).thenReturn(tokenDto);

		//then
		mockMvc.perform(post("/api/v1/oauth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf())
				.content(mapper.writeValueAsString(oauthRequest)))
			.andExpect(status().isBadRequest())
			.andExpect(result -> assertTrue(
				result.getResolvedException() instanceof MethodArgumentNotValidException))
			.andExpect(jsonPath("$.errors").exists());
	}

	@Test
	@DisplayName("유저는 토큰을 재발급 받을 수 있다.")
	void user_reissue_test() throws Exception {

		//given
		String reissueRefreshToken = "refresh-token";

		TokenDto newTokenDto = TokenDto.builder()
			.accessToken("new-access-token")
			.refreshToken("new-refresh-token")
			.build();

		//when
		when(oauthService.refresh(reissueRefreshToken)).thenReturn(newTokenDto);

		//then
		mockMvc.perform(post("/api/v1/oauth/reissue")
				.contentType(MediaType.APPLICATION_JSON)
				.header("refresh-token", reissueRefreshToken)
				.with(csrf())
				.content(mapper.writeValueAsString(reissueRefreshToken)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
			.andExpect(cookie().value("refresh-token", "new-refresh-token"))
			.andExpect(cookie().httpOnly("refresh-token", true))
			.andExpect(cookie().secure("refresh-token", true))
			.andDo(print());
	}

	@Test
	@DisplayName("refresh 토큰이 유효하지 않으면 토큰을 재발급 받을 수 없다.")
	void user_reissue_fail_when_refreshToken_is_not_invalid() throws Exception {

		String reissueRefreshToken = "refresh-tokenxzz";

		TokenDto newTokenDto = TokenDto.builder()
			.accessToken("new-access-token")
			.refreshToken("new-refresh-token")
			.build();
		//when
		when(oauthService.refresh(reissueRefreshToken)).thenThrow(
			new UserException(UserExceptionCode.INVALID_REFRESH_TOKEN));

		//then
		mockMvc.perform(post("/api/v1/oauth/reissue")
				.contentType(MediaType.APPLICATION_JSON)
				.header("refresh-token", reissueRefreshToken)
				.with(csrf()))
			.andExpect(status().isBadRequest())
			.andExpect(result -> assertTrue(
				result.getResolvedException() instanceof UserException))
			.andExpect(jsonPath("$.errors").exists());
	}

	@Test
	@DisplayName("헤더의 리프레쉬 토큰이 null이면 토큰을 재발급 받을 수 없다.")
	void user_reissue_fail_when_refreshToken_is_null() throws Exception {
		String reissueRefreshToken = null;

		// when
		when(oauthService.refresh(reissueRefreshToken)).thenThrow(
			new IllegalArgumentException("Refresh token is missing"));

		// then
		mockMvc.perform(post("/api/v1/oauth/reissue")
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf()))
			.andExpect(status().isUnauthorized())
			.andExpect(result -> assertTrue(
				result.getResolvedException() instanceof IllegalArgumentException))
			.andExpect(jsonPath("$.errors").exists());
	}


}
