package app.bottlenote.user.integration;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.global.data.response.Error;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.user.domain.constant.SocialType;
import app.bottlenote.user.dto.request.OauthRequest;
import app.bottlenote.user.dto.response.MyPageResponse;
import app.bottlenote.user.exception.UserExceptionCode;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("integration")
@DisplayName("[integration] [controller] UserQueryController")
class UserQueryIntegrationTest extends IntegrationTestSupport {

	private String loginAndGetAccessToken() throws Exception {
		OauthRequest oauthRequest = new OauthRequest("hyejj19@naver.com", SocialType.KAKAO, null, null);

		MvcResult loginResult = mockMvc.perform(post("/api/v1/oauth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf())
				.content(mapper.writeValueAsString(oauthRequest)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.data.accessToken").exists())
			.andReturn();

		String loginResponseString = loginResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
		JsonNode loginResponseJson = mapper.readTree(loginResponseString);

		return loginResponseJson.path("data").path("accessToken").asText();
	}

	@DisplayName("로그인 유저가 타인의 마이페이지를 조회할 수 있다.")
	@Sql(scripts = {"/init-script/init-user-mypage-query.sql"})
	@Test
	void test_1() throws Exception {
		String accessToken = loginAndGetAccessToken();
		final Long userId = 2L;

		MvcResult result = mockMvc.perform(get("/api/v1/mypage/{userId}", userId)
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf())
				.header("Authorization", "Bearer " + accessToken))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.data").exists())
			.andExpect(jsonPath("$.data.userId").value(userId))
			.andReturn();

		// 응답 데이터를 검증하고 로그 출력
		String responseString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		GlobalResponse response = mapper.readValue(responseString, GlobalResponse.class);
		MyPageResponse myPageResponse = mapper.convertValue(response.getData(), MyPageResponse.class);
		log.info(myPageResponse.toString());
	}

	@DisplayName("로그인 유저가 자신의 마이페이지를 조회할 수 있다.")
	@Sql(scripts = {"/init-script/init-user-mypage-query.sql"})
	@Test
	void test_2() throws Exception {

		String accessToken = loginAndGetAccessToken();

		final Long userId = 1L;

		MvcResult result = mockMvc.perform(get("/api/v1/mypage/{userId}", userId)
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf())
				.header("Authorization", "Bearer " + accessToken))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.data").exists())
			.andExpect(jsonPath("$.data.userId").value(userId))
			.andExpect(jsonPath("$.data.isMyPage").value(true))
			.andReturn();
	}

	@DisplayName("비회원 유저가 타인의 마이페이지를 조회할 수 있다.")
	@Sql(scripts = {"/init-script/init-user-mypage-query.sql"})
	@Test
	void test_3() throws Exception {

		final Long userId = 2L;

		MvcResult result = mockMvc.perform(get("/api/v1/mypage/{userId}", userId)
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf()))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.data").exists())
			.andExpect(jsonPath("$.data.userId").value(userId))
			.andReturn();
	}

	@DisplayName("마이페이지 유저가 존재하지 않는 경우")
	@Sql(scripts = {"/init-script/init-user-mypage-query.sql"})
	@Test
	void test_4() throws Exception {
		Error error = Error.of(UserExceptionCode.MYPAGE_NOT_ACCESSIBLE);
		final Long userId = 999L;  // 존재하지 않는 유저 ID
		mockMvc.perform(get("/api/v1/mypage/{userId}", userId)
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf()))
			.andDo(print())
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.errors[0].code").value(String.valueOf(error.code())))
			.andExpect(jsonPath("$.errors[0].status").value(error.status().name()))
			.andExpect(jsonPath("$.errors[0].message").value(error.message()));

	}
}
