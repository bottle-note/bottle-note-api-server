package app.bottlenote.user.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.user.domain.constant.GenderType;
import app.bottlenote.user.domain.constant.SocialType;
import app.bottlenote.user.dto.request.OauthRequest;
import app.bottlenote.user.dto.request.TokenRequest;
import app.bottlenote.user.dto.response.OauthResponse;
import app.bottlenote.user.service.OauthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@DisplayName("유저 로그인 컨트롤러 테스트")
@WebMvcTest(OauthController.class)
@WithMockUser
class UserControllerTest {

	@Autowired
	protected ObjectMapper mapper;
	@Autowired
	protected MockMvc mockMvc;
	@MockBean
	protected OauthService oauthService;

	@Test
	@DisplayName("유저는 로그인 할 수 있다.")
	void user_login_test() throws Exception {

		//given
		OauthRequest oauthRequest = new OauthRequest("cdm2883@naver.com", SocialType.KAKAO,
			GenderType.MALE,
			27);

		OauthResponse oauthResponse = new OauthResponse("accessToken", "refreshToken");

		//when
		when(oauthService.oauthLogin(oauthRequest)).thenReturn(oauthResponse);

		//then
		mockMvc.perform(post("/api/v1/oauth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf())
				.content(mapper.writeValueAsString(oauthRequest)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.data.accessToken").value("accessToken"))
			.andExpect(jsonPath("$.data.refreshToken").value("refreshToken"))
			.andDo(print());
	}

	@Test
	@DisplayName("유저는 토큰을 재발급 받을 수 있다.")
	void user_reissue_test() throws Exception {

		//given
		TokenRequest tokenRequest = new TokenRequest("access-token", "refresh-token");

		OauthResponse oauthResponse = new OauthResponse("new-access-token", "new-refresh-token");

		//when
		when(oauthService.refresh(tokenRequest)).thenReturn(oauthResponse);

		//then
		mockMvc.perform(post("/api/v1/oauth/reissue")
				.contentType(MediaType.APPLICATION_JSON)
				.header("accessToken", tokenRequest.accessToken())
				.header("refreshToken", tokenRequest.refreshToken())
				.with(csrf())
				.content(mapper.writeValueAsString(tokenRequest)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
			.andExpect(jsonPath("$.data.refreshToken").value("new-refresh-token"))
			.andDo(print());
	}

}
