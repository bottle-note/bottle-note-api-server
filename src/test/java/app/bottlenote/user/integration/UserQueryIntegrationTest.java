package app.bottlenote.user.integration;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.global.data.response.Error;
import app.bottlenote.user.exception.UserExceptionCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("integration")
@DisplayName("[integration] [controller] UserQueryController")
class UserQueryIntegrationTest extends IntegrationTestSupport {

	@Nested
	@DisplayName("마이페이지")
	class myPage {

		@DisplayName("로그인 유저가 타인의 마이페이지를 조회할 수 있다.")
		@Sql(scripts = {"/init-script/init-user-mypage-query.sql"})
		@Test
		void test_1() throws Exception {

			String accessToken = getToken();
			Long userId = 2L;
			Long requestUserId = getTokenUserId();

			mockMvc.perform(get("/api/v1/my-page/{userId}", userId)
					.contentType(MediaType.APPLICATION_JSON)
					.with(csrf())
					.header("Authorization", "Bearer " + accessToken))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data").exists())
				.andExpect(jsonPath("$.data.userId").value(userId))
				.andReturn();

			assertNotEquals(userId, requestUserId);

		}

		@DisplayName("로그인 유저가 자신의 마이페이지를 조회할 수 있다.")
		@Sql(scripts = {"/init-script/init-user-mypage-query.sql"})
		@Test
		void test_2() throws Exception {

			String accessToken = getToken();
			Long userId = getTokenUserId();

			mockMvc.perform(get("/api/v1/my-page/{userId}", userId)
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

			mockMvc.perform(get("/api/v1/my-page/{userId}", userId)
					.contentType(MediaType.APPLICATION_JSON)
					.with(csrf()))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data").exists())
				.andExpect(jsonPath("$.data.userId").value(userId))
				.andReturn();
		}

		@DisplayName("유저가 존재하지 않는 경우 MYPAGE_NOT_ACCESSIBLE 에러를 발생한다.")
		@Sql(scripts = {"/init-script/init-user-mypage-query.sql"})
		@Test
		void test_4() throws Exception {
			Error error = Error.of(UserExceptionCode.MYPAGE_NOT_ACCESSIBLE);
			final Long userId = 999L;  // 존재하지 않는 유저 ID
			mockMvc.perform(get("/api/v1/my-page/{userId}", userId)
					.contentType(MediaType.APPLICATION_JSON)
					.with(csrf()))
				.andDo(print())
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errors[0].code").value(String.valueOf(error.code())))
				.andExpect(jsonPath("$.errors[0].status").value(error.status().name()))
				.andExpect(jsonPath("$.errors[0].message").value(error.message()));
		}
	}

	@Nested
	@DisplayName("마이보틀")
	class myBottle {

		@DisplayName("로그인 유저가 타인의 마이보틀을 조회할 수 있다.")
		@Sql(scripts = {"/init-script/init-user-mybottle-query.sql"})
		@Test
		void test_1() throws Exception {

			String accessToken = getToken();
			Long userId = 2L;
			Long requestUserId = getTokenUserId();

			mockMvc.perform(get("/api/v1/my-page/{userId}/my-bottle", userId)
					.param("keyword", "")
					.param("regionId", "")
					.param("tabType", "ALL")
					.param("sortType", "LATEST")
					.param("sortOrder", "DESC")
					.param("cursor", "0")
					.param("pageSize", "50")
					.contentType(MediaType.APPLICATION_JSON)
					.header("Authorization", "Bearer " + accessToken)
					.with(csrf()))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data").exists())
				.andReturn();

			assertNotEquals(userId, requestUserId);

		}

		@DisplayName("로그인 유저가 자신의 마이보틀을 조회할 수 있다.")
		@Sql(scripts = {"/init-script/init-user-mybottle-query.sql"})
		@Test
		void test_2() throws Exception {

			String accessToken = getToken();
			Long userId = getTokenUserId();

			mockMvc.perform(get("/api/v1/my-page/{userId}/my-bottle", userId)
					.param("keyword", "")
					.param("regionId", "")
					.param("tabType", "ALL")
					.param("sortType", "LATEST")
					.param("sortOrder", "DESC")
					.param("cursor", "0")
					.param("pageSize", "50")
					.contentType(MediaType.APPLICATION_JSON)
					.header("Authorization", "Bearer " + accessToken)
					.with(csrf()))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data").exists())
				.andExpect(jsonPath("$.data.userId").value(userId))
				.andExpect(jsonPath("$.data.isMyPage").value(true))
				.andReturn();
		}

		@DisplayName("비회원 유저는 조회하면 BAD_REQUEST 예외를 반환한다.")
		@Sql(scripts = {"/init-script/init-user-mybottle-query.sql"})
		@Test
		void test_3() throws Exception {

			final Long userId = 2L;

			mockMvc.perform(get("/api/v1/my-page/{userId}/my-bottle", userId)
					.param("keyword", "")
					.param("regionId", "")
					.param("tabType", "ALL")
					.param("sortType", "LATEST")
					.param("sortOrder", "DESC")
					.param("cursor", "0")
					.param("pageSize", "50")
					.contentType(MediaType.APPLICATION_JSON)
					.with(csrf()))
				.andDo(print())
				.andExpect(status().isBadRequest()); // 비회원은 접근 불가
		}

		@DisplayName("마이보틀 유저가 존재하지 않는 경우 REQUIRED_USER_ID 예외를 반환한다.")
		@Sql(scripts = {"/init-script/init-user-mybottle-query.sql"})
		@Test
		void test_4() throws Exception {
			Error error = Error.of(UserExceptionCode.REQUIRED_USER_ID);
			final Long userId = 999L; // 존재하지 않는 유저 ID

			mockMvc.perform(get("/api/v1/my-page/{userId}/my-bottle", userId)
					.param("keyword", "")
					.param("regionId", "")
					.param("tabType", "ALL")
					.param("sortType", "LATEST")
					.param("sortOrder", "DESC")
					.param("cursor", "0")
					.param("pageSize", "50")
					.contentType(MediaType.APPLICATION_JSON)
					.with(csrf()))
				.andDo(print())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].code").value(String.valueOf(error.code())))
				.andExpect(jsonPath("$.errors[0].status").value(error.status().name()))
				.andExpect(jsonPath("$.errors[0].message").value(error.message()));
		}

	}

}
