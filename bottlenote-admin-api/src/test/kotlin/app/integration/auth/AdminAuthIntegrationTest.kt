package app.integration.auth

import app.IntegrationTestSupport
import app.bottlenote.user.constant.AdminRole
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType

@Tag("admin_integration")
@DisplayName("[integration] Admin Auth API 통합 테스트")
class AdminAuthIntegrationTest : IntegrationTestSupport() {

	@Nested
	@DisplayName("로그인 API")
	inner class LoginTest {

		@Test
		@DisplayName("올바른 이메일과 비밀번호로 로그인에 성공한다")
		fun loginSuccess() {
			// given
			val email = "test@bottlenote.com"
			val password = "password123"
			adminUserTestFactory.persistRootAdmin(email, password)

			val request = mapOf("email" to email, "password" to password)

			// when & then
			assertThat(
				mockMvcTester.post()
					.uri("/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.success").isEqualTo(true)

			assertThat(
				mockMvcTester.post()
					.uri("/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.accessToken").isNotNull()
		}

		@Test
		@DisplayName("잘못된 비밀번호로 로그인 시 실패한다")
		fun loginFailWithWrongPassword() {
			// given
			val email = "test@bottlenote.com"
			val password = "password123"
			adminUserTestFactory.persistRootAdmin(email, password)

			val request = mapOf("email" to email, "password" to "wrongPassword")

			// when & then
			assertThat(
				mockMvcTester.post()
					.uri("/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			)
				.hasStatus4xxClientError()
				.bodyJson()
				.extractingPath("$.success").isEqualTo(false)
		}

		@Test
		@DisplayName("존재하지 않는 이메일로 로그인 시 실패한다")
		fun loginFailWithNonExistentEmail() {
			// given
			val request = mapOf("email" to "nonexistent@bottlenote.com", "password" to "password123")

			// when & then
			assertThat(
				mockMvcTester.post()
					.uri("/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			)
				.hasStatus4xxClientError()
				.bodyJson()
				.extractingPath("$.success").isEqualTo(false)
		}

		@Test
		@DisplayName("비활성화된 어드민 계정으로 로그인 시 실패한다")
		fun loginFailWithInactiveAdmin() {
			// given
			val email = "inactive@bottlenote.com"
			val password = "password123"
			adminUserTestFactory.persistInactiveAdmin(email, password)

			val request = mapOf("email" to email, "password" to password)

			// when & then
			assertThat(
				mockMvcTester.post()
					.uri("/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			)
				.hasStatus4xxClientError()
				.bodyJson()
				.extractingPath("$.success").isEqualTo(false)
		}

		@Test
		@DisplayName("다중 역할을 가진 어드민도 로그인에 성공한다")
		fun loginSuccessWithMultipleRoles() {
			// given
			val roles = listOf(AdminRole.PARTNER, AdminRole.COMMUNITY_MANAGER)
			val admin = adminUserTestFactory.persistMultiRoleAdmin(roles)

			val request = mapOf("email" to admin.email, "password" to "password123")

			// when & then
			assertThat(
				mockMvcTester.post()
					.uri("/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.success").isEqualTo(true)
		}
	}

	@Nested
	@DisplayName("토큰 갱신 API")
	inner class RefreshTest {

		@Test
		@DisplayName("유효한 리프레시 토큰으로 토큰 갱신에 성공한다")
		fun refreshSuccess() {
			// given
			val email = "refresh-test@bottlenote.com"
			val password = "password123"
			adminUserTestFactory.persistRootAdmin(email, password)

			// 로그인해서 토큰 획득
			val loginRequest = mapOf("email" to email, "password" to password)
			val loginResult = mockMvcTester.post()
				.uri("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(loginRequest))
				.exchange()

			val loginResponse = mapper.readTree(loginResult.response.contentAsString)
			val refreshToken = loginResponse.path("data").path("refreshToken").asText()

			val request = mapOf("refreshToken" to refreshToken)

			// when & then
			assertThat(
				mockMvcTester.post()
					.uri("/auth/refresh")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.accessToken").isNotNull()
		}

		@Test
		@DisplayName("유효하지 않은 리프레시 토큰으로 갱신 시 실패한다")
		fun refreshFailWithInvalidToken() {
			// given
			val request = mapOf("refreshToken" to "invalid.refresh.token")

			// when & then
			assertThat(
				mockMvcTester.post()
					.uri("/auth/refresh")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			)
				.hasStatus4xxClientError()
				.bodyJson()
				.extractingPath("$.success").isEqualTo(false)
		}
	}

	@Nested
	@DisplayName("탈퇴 API")
	inner class WithdrawTest {

		@Test
		@DisplayName("인증된 어드민이 탈퇴에 성공한다")
		fun withdrawSuccess() {
			// given
			val admin = adminUserTestFactory.persistRootAdmin()
			val accessToken = getAccessToken(admin)

			// when & then
			assertThat(
				mockMvcTester.delete()
					.uri("/auth/withdraw")
					.header("Authorization", "Bearer $accessToken")
			)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.message").isEqualTo("탈퇴 처리되었습니다.")
		}
	}

	@Nested
	@DisplayName("회원가입 API")
	inner class SignupTest {

		@Test
		@DisplayName("인증된 어드민이 새 어드민 계정을 생성할 수 있다")
		fun signupSuccess() {
			// given
			val admin = adminUserTestFactory.persistRootAdmin()
			val accessToken = getAccessToken(admin)

			val request = mapOf(
				"email" to "new@bottlenote.com",
				"password" to "password123",
				"name" to "새 어드민",
				"roles" to listOf("PARTNER")
			)

			// when & then
			assertThat(
				mockMvcTester.post()
					.uri("/auth/signup")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.success").isEqualTo(true)

			assertThat(
				mockMvcTester.post()
					.uri("/auth/signup")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request.plus("email" to "another@bottlenote.com")))
			)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.email").isEqualTo("another@bottlenote.com")
		}

		@Test
		@DisplayName("다중 역할을 가진 어드민 계정을 생성할 수 있다")
		fun signupWithMultipleRoles() {
			// given
			val admin = adminUserTestFactory.persistRootAdmin()
			val accessToken = getAccessToken(admin)

			val request = mapOf(
				"email" to "multi-role@bottlenote.com",
				"password" to "password123",
				"name" to "다중 역할 어드민",
				"roles" to listOf("PARTNER", "COMMUNITY_MANAGER")
			)

			// when & then
			assertThat(
				mockMvcTester.post()
					.uri("/auth/signup")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.roles").isEqualTo(listOf("PARTNER", "COMMUNITY_MANAGER"))
		}

		@Test
		@DisplayName("ROOT_ADMIN이 ROOT_ADMIN 역할을 포함한 어드민을 생성할 수 있다")
		fun rootAdminCanCreateRootAdmin() {
			// given
			val admin = adminUserTestFactory.persistRootAdmin()
			val accessToken = getAccessToken(admin)

			val request = mapOf(
				"email" to "new-root@bottlenote.com",
				"password" to "password123",
				"name" to "새 루트 어드민",
				"roles" to listOf("ROOT_ADMIN")
			)

			// when & then
			assertThat(
				mockMvcTester.post()
					.uri("/auth/signup")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.roles").isEqualTo(listOf("ROOT_ADMIN"))
		}

		@Test
		@DisplayName("ROOT_ADMIN이 아닌 어드민이 ROOT_ADMIN 역할을 부여하려 하면 실패한다")
		fun nonRootAdminCannotAssignRootAdminRole() {
			// given
			val admin = adminUserTestFactory.persistPartnerAdmin()
			val accessToken = getAccessToken(admin)

			val request = mapOf(
				"email" to "root-attempt@bottlenote.com",
				"password" to "password123",
				"name" to "루트 시도",
				"roles" to listOf("ROOT_ADMIN")
			)

			// when & then
			assertThat(
				mockMvcTester.post()
					.uri("/auth/signup")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			)
				.hasStatus4xxClientError()
				.bodyJson()
				.extractingPath("$.success").isEqualTo(false)
		}

		@Test
		@DisplayName("인증되지 않은 사용자는 회원가입 API를 호출할 수 없다")
		fun signupFailWithoutAuth() {
			// given
			val request = mapOf(
				"email" to "no-auth@bottlenote.com",
				"password" to "password123",
				"name" to "인증 없는 사용자",
				"roles" to listOf("PARTNER")
			)

			// when & then
			assertThat(
				mockMvcTester.post()
					.uri("/auth/signup")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			)
				.hasStatus4xxClientError()
		}

		@Test
		@DisplayName("중복된 이메일로 회원가입 시 실패한다")
		fun signupFailWithDuplicateEmail() {
			// given
			val existingEmail = "existing@bottlenote.com"
			adminUserTestFactory.persistAdminUser(existingEmail, "password123", "기존 어드민", listOf(AdminRole.PARTNER))

			val admin = adminUserTestFactory.persistRootAdmin()
			val accessToken = getAccessToken(admin)

			val request = mapOf(
				"email" to existingEmail,
				"password" to "password123",
				"name" to "중복 시도",
				"roles" to listOf("PARTNER")
			)

			// when & then
			assertThat(
				mockMvcTester.post()
					.uri("/auth/signup")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			)
				.hasStatus4xxClientError()
				.bodyJson()
				.extractingPath("$.success").isEqualTo(false)
		}

		@Test
		@DisplayName("역할이 비어있으면 회원가입 시 실패한다")
		fun signupFailWithEmptyRoles() {
			// given
			val admin = adminUserTestFactory.persistRootAdmin()
			val accessToken = getAccessToken(admin)

			val request = mapOf(
				"email" to "empty-roles@bottlenote.com",
				"password" to "password123",
				"name" to "역할 없음",
				"roles" to emptyList<String>()
			)

			// when & then
			assertThat(
				mockMvcTester.post()
					.uri("/auth/signup")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			)
				.hasStatus4xxClientError()
		}
	}
}
