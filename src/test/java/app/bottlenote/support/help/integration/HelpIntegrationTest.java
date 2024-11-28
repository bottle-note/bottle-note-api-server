package app.bottlenote.support.help.integration;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.global.data.response.Error;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.support.help.domain.constant.HelpType;
import app.bottlenote.support.help.dto.HelpImageInfo;
import app.bottlenote.support.help.dto.request.HelpUpsertRequest;
import app.bottlenote.support.help.dto.response.HelpDetailInfo;
import app.bottlenote.support.help.dto.response.HelpListResponse;
import app.bottlenote.support.help.dto.response.HelpResultResponse;
import app.bottlenote.support.help.fixture.HelpObjectFixture;
import app.bottlenote.support.help.repository.HelpRepository;
import app.bottlenote.user.domain.constant.SocialType;
import app.bottlenote.user.dto.request.OauthRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static app.bottlenote.global.exception.custom.code.ValidExceptionCode.CONTENT_NOT_EMPTY;
import static app.bottlenote.global.exception.custom.code.ValidExceptionCode.REQUIRED_HELP_TYPE;
import static app.bottlenote.support.help.dto.response.constant.HelpResultMessage.DELETE_SUCCESS;
import static app.bottlenote.support.help.dto.response.constant.HelpResultMessage.MODIFY_SUCCESS;
import static app.bottlenote.support.help.dto.response.constant.HelpResultMessage.REGISTER_SUCCESS;
import static app.bottlenote.support.help.exception.HelpExceptionCode.HELP_NOT_AUTHORIZED;
import static app.bottlenote.support.help.exception.HelpExceptionCode.HELP_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("integration")
@DisplayName("[integration] [controller] HelpController")
@WithMockUser
class HelpIntegrationTest extends IntegrationTestSupport {

	private OauthRequest oauthRequest;
	private HelpUpsertRequest helpUpsertRequest;


	@Autowired
	private HelpRepository helpRepository;

	@BeforeEach
	void setUp() {
		oauthRequest = new OauthRequest("chadongmin@naver.com", SocialType.KAKAO, null, null);
		helpUpsertRequest = HelpObjectFixture.getHelpUpsertRequest();

	}

	@DisplayName("Not null 필드에 null이 할당되면 예외를 반환한다.")
	@Test
	void test_1() throws Exception {

		Error error = Error.of(REQUIRED_HELP_TYPE);

		helpUpsertRequest = new HelpUpsertRequest("로그인이 안돼요", null, List.of(new HelpImageInfo(1L, "https://test.com")));
		// given when
		mockMvc.perform(post("/api/v1/help")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsBytes(helpUpsertRequest))
				.header("Authorization", "Bearer " + getToken(oauthRequest).accessToken())
				.with(csrf())
			)
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[?(@.code == 'REQUIRED_HELP_TYPE')].status").value(error.status().name()))
			.andExpect(jsonPath("$.errors[?(@.code == 'REQUIRED_HELP_TYPE')].message").value(error.message()));
	}

	@Nested
	@DisplayName("[Integration] 문의글 작성 통합테스트")
	class HelpRegisterControllerIntegrationTest extends IntegrationTestSupport {

		@DisplayName("문의글을 작성할 수 있다.")
		@Test
		void test_1() throws Exception {
			// given when
			MvcResult result = mockMvc.perform(post("/api/v1/help")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsBytes(helpUpsertRequest))
					.header("Authorization", "Bearer " + getToken(oauthRequest).accessToken())
					.with(csrf())
				)
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data").exists())
				.andReturn();

			String contentAsString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
			GlobalResponse response = mapper.readValue(contentAsString, GlobalResponse.class);
			HelpResultResponse helpResultResponse = mapper.convertValue(response.getData(), HelpResultResponse.class);

			//then
			assertEquals(REGISTER_SUCCESS, helpResultResponse.codeMessage());
		}
	}

	@Sql(scripts = {
		"/init-script/init-user.sql",
		"/init-script/init-help.sql"})
	@Nested
	@DisplayName("[Integration] 문의글 조회 통합테스트")
	class HelpReadIntegrationTest extends IntegrationTestSupport {

		@DisplayName("문의글 목록을 조회할 수 있다.")
		@Test
		void test_1() throws Exception {
			// given

			MvcResult result = mockMvc.perform(get("/api/v1/help")
					.contentType(MediaType.APPLICATION_JSON)
					.header("Authorization", "Bearer " + getToken(oauthRequest).accessToken())
					.with(csrf())
				)
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data").exists())
				.andReturn();

			String contentAsString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
			GlobalResponse response = mapper.readValue(contentAsString, GlobalResponse.class);
			HelpListResponse helpListResponse = mapper.convertValue(response.getData(), HelpListResponse.class);

			assertEquals(1, helpListResponse.totalCount());
		}

		@DisplayName("문의글 상세 조회할 수 있다.")
		@Test
		void test_2() throws Exception {
			// given

			MvcResult result = mockMvc.perform(get("/api/v1/help/{helpId}", 1L)
					.contentType(MediaType.APPLICATION_JSON)
					.header("Authorization", "Bearer " + getToken(oauthRequest).accessToken())
					.with(csrf())
				)
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data").exists())
				.andReturn();

			String contentAsString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
			GlobalResponse response = mapper.readValue(contentAsString, GlobalResponse.class);
			HelpDetailInfo helpDetailInfo = mapper.convertValue(response.getData(), HelpDetailInfo.class);

			assertEquals(HelpType.USER, helpDetailInfo.helpType());
		}
	}

	@Sql(scripts = {
		"/init-script/init-user.sql",
		"/init-script/init-help.sql"})
	@Nested
	@DisplayName("[Integration] 문의글 수정 통합테스트")
	class HelpModifyIntegrationTest extends IntegrationTestSupport {

		@DisplayName("문의글을 수정할 수 있다.")
		@Test
		void test_1() throws Exception {
			// given when
			long helpId = 1L;
			MvcResult result = mockMvc.perform(patch("/api/v1/help/{helpId}", helpId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsBytes(helpUpsertRequest))
					.header("Authorization", "Bearer " + getToken(oauthRequest).accessToken())
					.with(csrf())
				)
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data").exists())
				.andReturn();

			String contentAsString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
			GlobalResponse response = mapper.readValue(contentAsString, GlobalResponse.class);
			HelpResultResponse helpResultResponse = mapper.convertValue(response.getData(), HelpResultResponse.class);

			//then
			assertEquals(MODIFY_SUCCESS, helpResultResponse.codeMessage());
		}

		@DisplayName("유저 본인이 작성한 글이 아니면 문의글을 수정할 수 없다.")
		@Test
		void test_2() throws Exception {
			// given when
			long helpId = 1L;
			oauthRequest = new OauthRequest("test@naver.com", SocialType.KAKAO, null, null);
			Error error = Error.of(HELP_NOT_AUTHORIZED);

			mockMvc.perform(patch("/api/v1/help/{helpId}", helpId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsBytes(helpUpsertRequest))
					.header("Authorization", "Bearer " + getToken(oauthRequest).accessToken())
					.with(csrf())
				)
				.andDo(print())
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errors[?(@.code == 'HELP_NOT_AUTHORIZED')].status").value(error.status().name()))
				.andExpect(jsonPath("$.errors[?(@.code == 'HELP_NOT_AUTHORIZED')].message").value(error.message()));
		}

		@DisplayName("존재하지 않는 문의글을 수정할 수 없다.")
		@Test
		void test_3() throws Exception {
			// given when
			long helpId = -1L;
			oauthRequest = new OauthRequest("test@naver.com", SocialType.KAKAO, null, null);
			Error error = Error.of(HELP_NOT_FOUND);

			mockMvc.perform(patch("/api/v1/help/{helpId}", helpId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsBytes(helpUpsertRequest))
					.header("Authorization", "Bearer " + getToken(oauthRequest).accessToken())
					.with(csrf())
				)
				.andDo(print())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[?(@.code == 'HELP_NOT_FOUND')].status").value(error.status().name()))
				.andExpect(jsonPath("$.errors[?(@.code == 'HELP_NOT_FOUND')].message").value(error.message()));
		}

		@DisplayName("Not null 필드에 null이 할당되면 예외를 반환한다.")
		@Test
		void test_4() throws Exception {

			long helpId = 1L;
			Error error = Error.of(CONTENT_NOT_EMPTY);

			helpUpsertRequest = new HelpUpsertRequest(null, HelpType.USER, List.of(new HelpImageInfo(1L, "https://test.com")));
			// given when
			mockMvc.perform(patch("/api/v1/help/{helpId}", helpId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsBytes(helpUpsertRequest))
					.header("Authorization", "Bearer " + getToken(oauthRequest).accessToken())
					.with(csrf())
				)
				.andDo(print())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[?(@.code == 'CONTENT_NOT_EMPTY')].status").value(error.status().name()))
				.andExpect(jsonPath("$.errors[?(@.code == 'CONTENT_NOT_EMPTY')].message").value(error.message()));
		}
	}

	@Sql(scripts = {
		"/init-script/init-user.sql",
		"/init-script/init-help.sql"})
	@Nested
	@DisplayName("[Integration] 문의글 수정 통합테스트")
	class HelpDeleteIntegrationTest extends IntegrationTestSupport {

		@DisplayName("문의글을 삭제할 수 있다.")
		@Test
		void test_1() throws Exception {
			// given when
			long helpId = 1L;
			MvcResult result = mockMvc.perform(delete("/api/v1/help/{helpId}", helpId)
					.contentType(MediaType.APPLICATION_JSON)
					.header("Authorization", "Bearer " + getToken(oauthRequest).accessToken())
					.with(csrf())
				)
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data").exists())
				.andReturn();

			String contentAsString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
			GlobalResponse response = mapper.readValue(contentAsString, GlobalResponse.class);
			HelpResultResponse helpResultResponse = mapper.convertValue(response.getData(), HelpResultResponse.class);

			//then
			assertEquals(DELETE_SUCCESS, helpResultResponse.codeMessage());
		}

		@DisplayName("존재하지 않는 문의글을 삭제할 수 없다.")
		@Test
		void test_2() throws Exception {
			// given when
			long helpId = -1L;
			oauthRequest = new OauthRequest("test@naver.com", SocialType.KAKAO, null, null);
			Error error = Error.of(HELP_NOT_FOUND);

			mockMvc.perform(delete("/api/v1/help/{helpId}", helpId)
					.contentType(MediaType.APPLICATION_JSON)
					.header("Authorization", "Bearer " + getToken(oauthRequest).accessToken())
					.with(csrf())
				)
				.andDo(print())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[?(@.code == 'HELP_NOT_FOUND')].status").value(error.status().name()))
				.andExpect(jsonPath("$.errors[?(@.code == 'HELP_NOT_FOUND')].message").value(error.message()));
		}

		@DisplayName("유저 본인이 작성한 글이 아니면 문의글을 삭제할 수 없다.")
		@Test
		void test_3() throws Exception {
			// given when
			long helpId = 1L;
			oauthRequest = new OauthRequest("test@naver.com", SocialType.KAKAO, null, null);
			Error error = Error.of(HELP_NOT_AUTHORIZED);

			mockMvc.perform(delete("/api/v1/help/{helpId}", helpId)
					.contentType(MediaType.APPLICATION_JSON)
					.header("Authorization", "Bearer " + getToken(oauthRequest).accessToken())
					.with(csrf())
				)
				.andDo(print())
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.errors[?(@.code == 'HELP_NOT_AUTHORIZED')].status").value(error.status().name()))
				.andExpect(jsonPath("$.errors[?(@.code == 'HELP_NOT_AUTHORIZED')].message").value(error.message()));
		}
	}
}
