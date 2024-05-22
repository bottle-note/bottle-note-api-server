package app.bottlenote.docs.user;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.docs.AbstractRestDocs;
import app.bottlenote.user.controller.OauthController;
import app.bottlenote.user.domain.constant.GenderType;
import app.bottlenote.user.domain.constant.SocialType;
import app.bottlenote.user.dto.request.OauthRequest;
import app.bottlenote.user.dto.response.TokenDto;
import app.bottlenote.user.service.OauthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

@DisplayName("유저 컨트롤러 RestDocs용 테스트")
class OauthControllerDocsTest extends AbstractRestDocs {

	private final OauthService oauthService = mock(OauthService.class);

	@Override
	protected Object initController() {
		return new OauthController(oauthService);
	}

	@Test
	@DisplayName("로그인을 할 수 있다.")
	void login_test() throws Exception {

		//given
		OauthRequest oauthRequest = new OauthRequest("cdm2883@naver.com", SocialType.KAKAO,
			GenderType.MALE,
			27);

		TokenDto tokenDto = TokenDto.builder()
			.accessToken(
				"eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJn44WB44WBZ2dAbmF2ZXIuY29tIiwicm9sZXMiOiJST0xFX1VTRVIiLCJ1c2VySWQiOjE2LCJpYXQiOjE3MTQ5NzU2MjMsImV4cCI6MTcxNDk3NjUyM30.41SuOBgmX-sd8nrMbC-xm0kH6rbny_SMYCKWE4rNQEZgSrRPS0HvYv0X7E-weo6sHlWWm1OmiQgHl4-uy6-9ig")
			.refreshToken(
				"eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJn44WB44WBZ2dAbmF2ZXIuY29tIiwicm9sZXMiOiJST0xFX1VTRVIiLCJ1c2VySWQiOjE2LCJpYXQiOjE3MTQ5NzU2MjMsImV4cCI6MTcxNjE4NTIyM30.lvmPueUcOb1erv5Llo4qhEUQ_gtWrpFGbBHDw-Pi94qj8MGojoEI3ugdMo8PwoKgrVQZ_gBwBbytwjxh8XktUg")
			.build();

		//when
		when(oauthService.oauthLogin(oauthRequest)).thenReturn(tokenDto);

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

		TokenDto newTokenDto = TokenDto.builder()
			.accessToken("new-access-token")
			.refreshToken("new-refresh-token")
			.build();

		//when
		when(oauthService.refresh(request)).thenReturn(newTokenDto);

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
}
