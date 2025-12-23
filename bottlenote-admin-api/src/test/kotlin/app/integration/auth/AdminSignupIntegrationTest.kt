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
@DisplayName("[integration] Admin Signup API 통합 테스트")
class AdminSignupIntegrationTest : IntegrationTestSupport() {

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
