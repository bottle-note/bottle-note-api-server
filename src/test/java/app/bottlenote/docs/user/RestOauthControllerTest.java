package app.bottlenote.docs.user;

import app.bottlenote.docs.AbstractRestDocs;
import app.bottlenote.user.config.OauthConfigProperties;
import app.bottlenote.user.constant.GenderType;
import app.bottlenote.user.constant.SocialType;
import app.bottlenote.user.controller.OauthController;
import app.bottlenote.user.dto.request.BasicAccountRequest;
import app.bottlenote.user.dto.request.BasicLoginRequest;
import app.bottlenote.user.dto.request.GuestCodeRequest;
import app.bottlenote.user.dto.request.OauthRequest;
import app.bottlenote.user.dto.response.BasicAccountResponse;
import app.bottlenote.user.dto.response.TokenItem;
import app.bottlenote.user.service.OauthService;
import app.external.push.data.request.SingleTokenRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import static java.util.Base64.getEncoder;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("document")
@DisplayName("유저 Auth 컨트롤러 RestDocs 테스트")
class RestOauthControllerTest extends AbstractRestDocs {
	private final OauthService oauthService = mock(OauthService.class);
	private final OauthConfigProperties config;

	public RestOauthControllerTest() {
		this.config = OauthConfigProperties.builder()
			.guestCode("TEST_GUEST_CODE")
			.cookieExpireTime(123456)
			.refreshTokenHeaderPrefix("refresh-token")
			.build();
	}

	@Override
	protected Object initController() {

		return new OauthController(oauthService, config);
	}

	@Test
	@DisplayName("로그인을 할 수 있다.")
	void login_test() throws Exception {

		//given
		OauthRequest oauthRequest = new OauthRequest("cdm2883@naver.com", SocialType.KAKAO, GenderType.MALE, 27);
		TokenItem tokenItem = TokenItem.builder()
			.accessToken("eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJn44WB44WBZ2dAbmF2ZXIuY29tIiwicm9sZXMiOiJST0xFX1VTRVIiLCJ1c2VySWQiOjE2LCJpYXQiOjE3MTQ5NzU2MjMsImV4cCI6MTcxNDk3NjUyM30.41SuOBgmX-sd8nrMbC-xm0kH6rbny_SMYCKWE4rNQEZgSrRPS0HvYv0X7E-weo6sHlWWm1OmiQgHl4-uy6-9ig")
			.refreshToken("eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJn44WB44WBZ2dAbmF2ZXIuY29tIiwicm9sZXMiOiJST0xFX1VTRVIiLCJ1c2VySWQiOjE2LCJpYXQiOjE3MTQ5NzU2MjMsImV4cCI6MTcxNjE4NTIyM30.lvmPueUcOb1erv5Llo4qhEUQ_gtWrpFGbBHDw-Pi94qj8MGojoEI3ugdMo8PwoKgrVQZ_gBwBbytwjxh8XktUg")
			.build();

		//when
		when(oauthService.login(oauthRequest)).thenReturn(tokenItem);

		//then
		mockMvc.perform(post("/api/v1/oauth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf())
				.content(objectMapper.writeValueAsString(oauthRequest)))
			.andExpect(status().isOk())
			.andExpect(cookie().exists("refresh-token"))

			.andDo(
				document("user/user-login",
					requestFields(
						fieldWithPath("email").type(JsonFieldType.STRING).description("이메일"),
						fieldWithPath("gender").type(JsonFieldType.STRING).description("성별")
							.optional(),
						fieldWithPath("age").type(JsonFieldType.NUMBER).description("나이")
							.optional(),
						fieldWithPath("socialType").type(JsonFieldType.STRING)
							.description("소셜 로그인 타입")
					),
					responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN)
							.description("응답 성공 여부"),
						fieldWithPath("code").type(JsonFieldType.NUMBER)
							.description("응답 코드(http status code)"),
						fieldWithPath("data.accessToken").type(JsonFieldType.STRING)
							.description("액세스 토큰"),
						fieldWithPath("errors").type(JsonFieldType.ARRAY)
							.description("응답 성공 여부가 false일 경우 에러 메시지(없을 경우 null)"),
						fieldWithPath("meta.serverEncoding").description("서버 인코딩 정도"),
						fieldWithPath("meta.serverVersion").description("서버 버전"),
						fieldWithPath("meta.serverPathVersion").description("서버 경로 버전"),
						fieldWithPath("meta.serverResponseTime").description("서버 응답 시간")
					),
					responseHeaders(
						headerWithName("Set-Cookie").description("리프레쉬 토큰")
					)
				)
			);
	}

	@Test
	@DisplayName("토큰 재발급을 할 수 있다.")
	void reissue_test() throws Exception {

		//given
		String request = "refresh-token";

		TokenItem newTokenItem = TokenItem.builder()
			.accessToken("new-access-token")
			.refreshToken("new-refresh-token")
			.build();

		//when
		when(oauthService.refresh(request)).thenReturn(newTokenItem);

		//then
		mockMvc.perform(post("/api/v1/oauth/reissue")
				.header("refresh-token", request)
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(cookie().exists("refresh-token"))

			.andDo(
				document("user/user-reissue",
					responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN)
							.description("응답 성공 여부"),
						fieldWithPath("code").type(JsonFieldType.NUMBER)
							.description("응답 코드(http status code)"),
						fieldWithPath("data.accessToken").type(JsonFieldType.STRING)
							.description("액세스 토큰"),
						fieldWithPath("errors").type(JsonFieldType.ARRAY)
							.description("응답 성공 여부가 false일 경우 에러 메시지(없을 경우 null)"),
						fieldWithPath("meta.serverEncoding").description("서버 인코딩 정도"),
						fieldWithPath("meta.serverVersion").description("서버 버전"),
						fieldWithPath("meta.serverPathVersion").description("서버 경로 버전"),
						fieldWithPath("meta.serverResponseTime").description("서버 응답 시간")
					),
					responseHeaders(
						headerWithName("Set-Cookie").description("리프레쉬 토큰")
					)
				)
			);


	}

	@Test
	@DisplayName("게스트 토큰을 발급 받을수 있다.")
	void guest_login() throws Exception {
		//given
		final String guestCode = config.getGuestCode();
		final var request = GuestCodeRequest.of(getEncoder().encodeToString(guestCode.getBytes()));
		final String token = "response-token";

		//when
		when(oauthService.guestLogin()).thenReturn(token);

		//then
		mockMvc.perform(post("/api/v1/oauth/guest-login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))
				.with(csrf()))
			.andExpect(status().isOk())
			.andDo(
				document("user/guest-login",
					requestFields(
						fieldWithPath("code").description("게스트 코드")
					),
					responseFields(
						fieldWithPath("success").ignored(),
						fieldWithPath("code").ignored(),
						fieldWithPath("errors").ignored(),
						fieldWithPath("data.accessToken").description("액세스 토큰"),
						fieldWithPath("meta.serverEncoding").description("서버 인코딩 정도"),
						fieldWithPath("meta.serverVersion").description("서버 버전"),
						fieldWithPath("meta.serverPathVersion").description("서버 경로 버전"),
						fieldWithPath("meta.serverResponseTime").description("서버 응답 시간")
					)
				)
			);


	}

	@Test
	@DisplayName("토큰 유효성을 검사할 수 있다.")
	void token_validation() throws Exception {
		//given
		final SingleTokenRequest request = new SingleTokenRequest("test-token");
		final String message = "Token is valid";

		//when
		when(oauthService.verifyToken(request.token())).thenReturn(message);

		//then
		mockMvc.perform(put("/api/v1/oauth/token/verify")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))
				.with(csrf()))
			.andExpect(status().isOk())
			.andDo(
				document("user/token-verify",
					requestFields(
						fieldWithPath("token").description("검사할 토큰")
					),
					responseFields(
						fieldWithPath("success").ignored(),
						fieldWithPath("code").ignored(),
						fieldWithPath("errors").ignored(),
						fieldWithPath("data").description("결과"),
						fieldWithPath("meta.serverEncoding").ignored(),
						fieldWithPath("meta.serverVersion").ignored(),
						fieldWithPath("meta.serverPathVersion").ignored(),
						fieldWithPath("meta.serverResponseTime").ignored()
					)
				)
			);
	}

	@Test
	@DisplayName("베이식 회원가입을 할 수 있다.")
	void basic_signup() throws Exception {
		//given
		final String nickName = "부드러운몰트";
		final BasicAccountRequest request = BasicAccountRequest.builder()
			.email("test@email.com")
			.password("test-password")
			.age(27)
			.gender(null)
			.build();

		final BasicAccountResponse response = BasicAccountResponse.builder()
			.message(nickName + "님 환영합니다!")
			.email(request.getEmail())
			.nickname(nickName)
			.accessToken("access-token")
			.refreshToken("refresh-token")
			.build();

		//when
		when(oauthService.basicSignup(request.getEmail(), request.getPassword(),
			request.getAge(), request.getGender())).thenReturn(response);

		//then
		mockMvc.perform(post("/api/v1/oauth/basic/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))
				.with(csrf()))
			.andExpect(status().isOk())
			.andDo(
				document("user/basic-signup",
					requestFields(
						fieldWithPath("email").description("이메일"),
						fieldWithPath("password").description("비밀번호"),
						fieldWithPath("age").description("나이"),
						fieldWithPath("gender").description("성별")
					),
					responseFields(
						fieldWithPath("success").ignored(),
						fieldWithPath("code").ignored(),
						fieldWithPath("errors").ignored(),
						fieldWithPath("data").description("결과"),
						fieldWithPath("data.message").description("결과 메시지"),
						fieldWithPath("data.email").description("이메일"),
						fieldWithPath("data.nickname").description("닉네임"),
						fieldWithPath("data.accessToken").description("accessToken"),
						fieldWithPath("data.refreshToken").description("refreshToken"),
						fieldWithPath("meta.serverEncoding").ignored(),
						fieldWithPath("meta.serverVersion").ignored(),
						fieldWithPath("meta.serverPathVersion").ignored(),
						fieldWithPath("meta.serverResponseTime").ignored()
					)
				)
			);
	}

	@Test
	@DisplayName("베이식 로그인을 할 수 있다.")
	void basic_login() throws Exception {
		//given
		final String nickName = "부드러운몰트";
		final BasicLoginRequest request = BasicLoginRequest.builder()
			.email("test@email.com")
			.password("test-password")
			.build();

		final TokenItem response = TokenItem.of("access-token", "refresh-token");
		//when
		when(oauthService.basicLogin(request.getEmail(), request.getPassword())).thenReturn(response);

		//then
		mockMvc.perform(post("/api/v1/oauth/basic/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))
				.with(csrf()))
			.andExpect(status().isOk())
			.andDo(
				document("user/basic-login",
					requestFields(
						fieldWithPath("email").description("이메일"),
						fieldWithPath("password").description("비밀번호")
					),
					responseFields(
						fieldWithPath("success").ignored(),
						fieldWithPath("code").ignored(),
						fieldWithPath("errors").ignored(),
						fieldWithPath("data").description("결과"),
						fieldWithPath("data.accessToken").description("accessToken"),
						fieldWithPath("meta.serverEncoding").ignored(),
						fieldWithPath("meta.serverVersion").ignored(),
						fieldWithPath("meta.serverPathVersion").ignored(),
						fieldWithPath("meta.serverResponseTime").ignored()
					)
				)
			);
	}

	@Test
	@DisplayName("회원 탈퇴를 복구 할 수 있다.")
	void restore() throws Exception {
		//given
		final String email = "test-email";
		final String password = "test-password";
		var request = BasicLoginRequest.builder()
			.email(email)
			.password(password)
			.build();

		doNothing().when(oauthService).restoreUser(email, password);

		mockMvc.perform(post("/api/v1/oauth/restore")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))
				.with(csrf()))
			.andExpect(status().isOk())
			.andDo(
				document("user/restore",
					requestFields(
						fieldWithPath("email").description("이메일"),
						fieldWithPath("password").description("비밀번호")
					),
					responseFields(
						fieldWithPath("success").ignored(),
						fieldWithPath("code").ignored(),
						fieldWithPath("errors").ignored(),
						fieldWithPath("data").description("결과 메시지"),
						fieldWithPath("meta.serverEncoding").ignored(),
						fieldWithPath("meta.serverVersion").ignored(),
						fieldWithPath("meta.serverPathVersion").ignored(),
						fieldWithPath("meta.serverResponseTime").ignored()
					)
				));
	}
}
