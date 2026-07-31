package app.integration.auth

import app.IntegrationTestSupport
import app.bottlenote.agent.domain.Agent
import app.bottlenote.agent.domain.AgentRepository
import app.bottlenote.agent.support.AgentKeyHasher
import app.bottlenote.common.constant.AuditPrincipalType
import app.bottlenote.global.annotation.SecurityPolicy.AuthType
import app.bottlenote.global.security.policy.SecurityPolicyRegistry
import app.bottlenote.user.constant.AdminRole
import app.bottlenote.user.constant.UserStatus
import app.bottlenote.user.domain.AdminUser
import app.bottlenote.user.domain.AdminUserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import java.util.UUID

@Tag("admin_integration")
@DisplayName("[integration] Admin Auth API 통합 테스트")
class AdminAuthIntegrationTest : IntegrationTestSupport() {
	@Autowired
	private lateinit var securityPolicyRegistry: SecurityPolicyRegistry

	@Autowired
	private lateinit var agentRepository: AgentRepository

	@Autowired
	private lateinit var adminUserRepository: AdminUserRepository

	@ParameterizedTest
	@CsvSource(
		"POST, /v1/auth/login, PUBLIC",
		"POST, /v1/auth/refresh, PUBLIC",
		"POST, /v1/auth/agent, PUBLIC",
		"POST, /v1/auth/signup, REQUIRED_AUTH",
		"DELETE, /v1/auth/withdraw, REQUIRED_AUTH",
		"GET, /v1/users, REQUIRED_AUTH",
		"GET, /v1/not-found, PUBLIC",
		"GET, /error, PUBLIC"
	)
	@DisplayName("admin-api SecurityPolicyRegistry가 실제 controller 정책을 수집한다")
	fun adminSecurityPolicyRegistryCollectsControllerPolicies(
		method: String,
		path: String,
		expected: String
	) {
		assertThat(securityPolicyRegistry.resolve(method, path)).isEqualTo(AuthType.valueOf(expected))
	}

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
				mockMvcTester
					.post()
					.uri("/v1/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			).hasStatusOk()
				.bodyJson()
				.extractingPath("$.success")
				.isEqualTo(true)

			assertThat(
				mockMvcTester
					.post()
					.uri("/v1/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			).hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.accessToken")
				.isNotNull()
		}

		@Test
		@DisplayName("잘못된 Authorization 헤더가 있어도 로그인에 성공한다")
		fun loginSuccessWithInvalidAuthorizationHeader() {
			// given
			val email = "invalid-header@bottlenote.com"
			val password = "password123"
			adminUserTestFactory.persistRootAdmin(email, password)

			val request = mapOf("email" to email, "password" to password)

			// when & then
			assertThat(
				mockMvcTester
					.post()
					.uri("/v1/auth/login")
					.header("Authorization", "Bearer invalid.token")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			).hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.accessToken")
				.isNotNull()
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
				mockMvcTester
					.post()
					.uri("/v1/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			).hasStatus4xxClientError()
				.bodyJson()
				.extractingPath("$.success")
				.isEqualTo(false)
		}

		@Test
		@DisplayName("존재하지 않는 이메일로 로그인 시 실패한다")
		fun loginFailWithNonExistentEmail() {
			// given
			val request = mapOf("email" to "nonexistent@bottlenote.com", "password" to "password123")

			// when & then
			assertThat(
				mockMvcTester
					.post()
					.uri("/v1/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			).hasStatus4xxClientError()
				.bodyJson()
				.extractingPath("$.success")
				.isEqualTo(false)
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
				mockMvcTester
					.post()
					.uri("/v1/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			).hasStatus4xxClientError()
				.bodyJson()
				.extractingPath("$.success")
				.isEqualTo(false)
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
				mockMvcTester
					.post()
					.uri("/v1/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			).hasStatusOk()
				.bodyJson()
				.extractingPath("$.success")
				.isEqualTo(true)
		}
	}

	@Nested
	@DisplayName("에이전트 로그인 API")
	inner class AgentLoginTest {
		private fun saveAgent(rawKey: String, profileCode: String, isActive: Boolean = true): Agent = agentRepository.save(
			Agent.builder()
				.id(UUID.randomUUID().toString())
				.profileCode(profileCode)
				.secretHash(AgentKeyHasher.normalizeAndHash(rawKey))
				.isActive(isActive)
				.build()
		)

		private fun saveAdminWithAgent(agentId: String, status: UserStatus = UserStatus.ACTIVE): AdminUser = adminUserRepository.save(
			AdminUser.builder()
				.email("agent-admin-${UUID.randomUUID()}@bottlenote.com")
				.password("encoded")
				.name("Agent Admin")
				.roles(listOf(AdminRole.ROOT_ADMIN))
				.status(status)
				.agentId(agentId)
				.build()
		)

		@Test
		@DisplayName("활성 에이전트 키로 매핑된 관리자 계정으로 로그인에 성공한다")
		fun agentLoginSuccess() {
			// given
			val rawKey = UUID.randomUUID().toString()
			val agent = saveAgent(rawKey, "9101")
			saveAdminWithAgent(agent.id)

			val request = mapOf("agentKey" to rawKey)

			// when & then
			assertThat(
				mockMvcTester
					.post()
					.uri("/v1/auth/agent")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			).hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.accessToken")
				.isNotNull()
		}

		@Test
		@DisplayName("에이전트 키가 UUID 형식이 아니면 400을 반환한다")
		fun agentLoginFailWithMalformedKey() {
			// given
			val request = mapOf("agentKey" to "not-a-uuid")

			// when & then
			assertThat(
				mockMvcTester
					.post()
					.uri("/v1/auth/agent")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			).hasStatus(HttpStatus.BAD_REQUEST)
		}

		@Test
		@DisplayName("등록되지 않은 에이전트 키는 401을 반환한다")
		fun agentLoginFailWithUnknownKey() {
			// given
			val request = mapOf("agentKey" to UUID.randomUUID().toString())

			// when & then
			assertThat(
				mockMvcTester
					.post()
					.uri("/v1/auth/agent")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			).hasStatus(HttpStatus.UNAUTHORIZED)
		}

		@Test
		@DisplayName("비활성 에이전트 키는 401을 반환한다")
		fun agentLoginFailWithInactiveAgent() {
			// given
			val rawKey = UUID.randomUUID().toString()
			saveAgent(rawKey, "9102", isActive = false)

			val request = mapOf("agentKey" to rawKey)

			// when & then
			assertThat(
				mockMvcTester
					.post()
					.uri("/v1/auth/agent")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			).hasStatus(HttpStatus.UNAUTHORIZED)
		}

		@Test
		@DisplayName("활성 에이전트라도 매핑된 관리자 계정이 없으면 401을 반환한다")
		fun agentLoginFailWithMissingMapping() {
			// given
			val rawKey = UUID.randomUUID().toString()
			saveAgent(rawKey, "9103")

			val request = mapOf("agentKey" to rawKey)

			// when & then
			assertThat(
				mockMvcTester
					.post()
					.uri("/v1/auth/agent")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			).hasStatus(HttpStatus.UNAUTHORIZED)
		}

		@Test
		@DisplayName("에이전트에 매핑된 관리자 계정이 비활성 상태면 401을 반환한다")
		fun agentLoginFailWithInactiveAdmin() {
			// given
			val rawKey = UUID.randomUUID().toString()
			val agent = saveAgent(rawKey, "9104")
			saveAdminWithAgent(agent.id, status = UserStatus.DELETED)

			val request = mapOf("agentKey" to rawKey)

			// when & then
			assertThat(
				mockMvcTester
					.post()
					.uri("/v1/auth/agent")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			).hasStatus(HttpStatus.UNAUTHORIZED)
		}

		@Test
		@DisplayName("에이전트 로그인으로 발급받은 토큰으로 보호된 관리자 API에 접근할 수 있다")
		fun agentIssuedTokenAccessesProtectedAdminApi() {
			// given
			val rawKey = UUID.randomUUID().toString()
			val agent = saveAgent(rawKey, "9105")
			saveAdminWithAgent(agent.id)

			val loginResult =
				mockMvcTester
					.post()
					.uri("/v1/auth/agent")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(mapOf("agentKey" to rawKey)))
					.exchange()
			val accessToken =
				mapper.readTree(loginResult.response.contentAsString).path("data").path("accessToken").asText()

			// when & then
			assertThat(
				mockMvcTester
					.get()
					.uri("/v1/users")
					.header("Authorization", "Bearer $accessToken")
			).hasStatusOk()
				.bodyJson()
				.extractingPath("$.success")
				.isEqualTo(true)
		}

		@Test
		@DisplayName("에이전트로 재로그인하면 이전 리프레시 토큰은 무효화된다(last-writer-wins)")
		fun agentReloginInvalidatesPreviousRefreshToken() {
			// given
			val rawKey = UUID.randomUUID().toString()
			val agent = saveAgent(rawKey, "9106")
			saveAdminWithAgent(agent.id)

			val firstLoginResult =
				mockMvcTester
					.post()
					.uri("/v1/auth/agent")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(mapOf("agentKey" to rawKey)))
					.exchange()
			val firstRefreshToken =
				mapper.readTree(firstLoginResult.response.contentAsString).path("data").path("refreshToken").asText()

			// JWT의 iat/exp는 초 단위라 같은 초에 재로그인하면 토큰이 동일해질 수 있어 시간차를 둔다
			Thread.sleep(1100)

			mockMvcTester
				.post()
				.uri("/v1/auth/agent")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(mapOf("agentKey" to rawKey)))
				.exchange()

			// when & then: 이전 refresh token은 더 이상 유효하지 않다
			assertThat(
				mockMvcTester
					.post()
					.uri("/v1/auth/refresh")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(mapOf("refreshToken" to firstRefreshToken)))
			).hasStatus4xxClientError()
		}

		@Test
		@DisplayName("에이전트 토큰으로 회원가입한 계정의 감사 주체는 기존과 동일하게 ADMIN으로 기록된다")
		fun agentIssuedTokenSignupAuditsAsAdminPrincipal() {
			// given
			val rawKey = UUID.randomUUID().toString()
			val agent = saveAgent(rawKey, "9107")
			val requester = saveAdminWithAgent(agent.id)

			val loginResult =
				mockMvcTester
					.post()
					.uri("/v1/auth/agent")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(mapOf("agentKey" to rawKey)))
					.exchange()
			val accessToken =
				mapper.readTree(loginResult.response.contentAsString).path("data").path("accessToken").asText()

			val newAdminEmail = "agent-signup-${UUID.randomUUID()}@bottlenote.com"
			val signupRequest =
				mapOf(
					"email" to newAdminEmail,
					"password" to "password123",
					"name" to "에이전트 발급 관리자",
					"roles" to listOf("PARTNER")
				)

			// when
			assertThat(
				mockMvcTester
					.post()
					.uri("/v1/auth/signup")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(signupRequest))
			).hasStatusOk()
				.bodyJson()
				.extractingPath("$.success")
				.isEqualTo(true)

			// then: 감사 주체는 AGENT가 아니라 기존과 동일하게 ADMIN으로 기록된다
			val createdAdmin = adminUserRepository.findByEmail(newAdminEmail).orElseThrow()
			assertThat(createdAdmin.createPrincipal.type).isEqualTo(AuditPrincipalType.ADMIN)
			assertThat(createdAdmin.createPrincipal.id).isEqualTo(requester.id)
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
			val loginResult =
				mockMvcTester
					.post()
					.uri("/v1/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(loginRequest))
					.exchange()

			val loginResponse = mapper.readTree(loginResult.response.contentAsString)
			val refreshToken = loginResponse.path("data").path("refreshToken").asText()

			val request = mapOf("refreshToken" to refreshToken)

			// when & then
			assertThat(
				mockMvcTester
					.post()
					.uri("/v1/auth/refresh")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			).hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.accessToken")
				.isNotNull()
		}

		@Test
		@DisplayName("잘못된 Authorization 헤더가 있어도 유효한 리프레시 토큰이면 갱신에 성공한다")
		fun refreshSuccessWithInvalidAuthorizationHeader() {
			// given
			val email = "refresh-invalid-header@bottlenote.com"
			val password = "password123"
			adminUserTestFactory.persistRootAdmin(email, password)

			val loginRequest = mapOf("email" to email, "password" to password)
			val loginResult =
				mockMvcTester
					.post()
					.uri("/v1/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(loginRequest))
					.exchange()

			val loginResponse = mapper.readTree(loginResult.response.contentAsString)
			val refreshToken = loginResponse.path("data").path("refreshToken").asText()
			val request = mapOf("refreshToken" to refreshToken)

			// when & then
			assertThat(
				mockMvcTester
					.post()
					.uri("/v1/auth/refresh")
					.header("Authorization", "Bearer invalid.token")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			).hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.accessToken")
				.isNotNull()
		}

		@Test
		@DisplayName("유효하지 않은 리프레시 토큰으로 갱신 시 실패한다")
		fun refreshFailWithInvalidToken() {
			// given
			val request = mapOf("refreshToken" to "invalid.refresh.token")

			// when & then
			assertThat(
				mockMvcTester
					.post()
					.uri("/v1/auth/refresh")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			).hasStatus4xxClientError()
				.bodyJson()
				.extractingPath("$.success")
				.isEqualTo(false)
		}
	}

	@Nested
	@DisplayName("탈퇴 API")
	inner class WithdrawTest {
		@Test
		@DisplayName("일반 어드민이 탈퇴에 성공한다")
		fun withdrawSuccess() {
			// given
			val admin = adminUserTestFactory.persistPartnerAdmin()
			val accessToken = getAccessToken(admin)

			// when & then
			assertThat(
				mockMvcTester
					.delete()
					.uri("/v1/auth/withdraw")
					.header("Authorization", "Bearer $accessToken")
			).hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.message")
				.isEqualTo("탈퇴 처리되었습니다.")
		}

		@Test
		@DisplayName("ROOT_ADMIN은 탈퇴할 수 없다")
		fun rootAdminCannotWithdraw() {
			// given
			val admin = adminUserTestFactory.persistRootAdmin()
			val accessToken = getAccessToken(admin)

			// when & then
			assertThat(
				mockMvcTester
					.delete()
					.uri("/v1/auth/withdraw")
					.header("Authorization", "Bearer $accessToken")
			).hasStatus4xxClientError()
				.bodyJson()
				.extractingPath("$.success")
				.isEqualTo(false)
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

			val request =
				mapOf(
					"email" to "new@bottlenote.com",
					"password" to "password123",
					"name" to "새 어드민",
					"roles" to listOf("PARTNER")
				)

			// when & then
			assertThat(
				mockMvcTester
					.post()
					.uri("/v1/auth/signup")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			).hasStatusOk()
				.bodyJson()
				.extractingPath("$.success")
				.isEqualTo(true)

			assertThat(
				mockMvcTester
					.post()
					.uri("/v1/auth/signup")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request.plus("email" to "another@bottlenote.com")))
			).hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.email")
				.isEqualTo("another@bottlenote.com")
		}

		@Test
		@DisplayName("다중 역할을 가진 어드민 계정을 생성할 수 있다")
		fun signupWithMultipleRoles() {
			// given
			val admin = adminUserTestFactory.persistRootAdmin()
			val accessToken = getAccessToken(admin)

			val request =
				mapOf(
					"email" to "multi-role@bottlenote.com",
					"password" to "password123",
					"name" to "다중 역할 어드민",
					"roles" to listOf("PARTNER", "COMMUNITY_MANAGER")
				)

			// when & then
			assertThat(
				mockMvcTester
					.post()
					.uri("/v1/auth/signup")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			).hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.roles")
				.isEqualTo(listOf("PARTNER", "COMMUNITY_MANAGER"))
		}

		@Test
		@DisplayName("ROOT_ADMIN이 ROOT_ADMIN 역할을 포함한 어드민을 생성할 수 있다")
		fun rootAdminCanCreateRootAdmin() {
			// given
			val admin = adminUserTestFactory.persistRootAdmin()
			val accessToken = getAccessToken(admin)

			val request =
				mapOf(
					"email" to "new-root@bottlenote.com",
					"password" to "password123",
					"name" to "새 루트 어드민",
					"roles" to listOf("ROOT_ADMIN")
				)

			// when & then
			assertThat(
				mockMvcTester
					.post()
					.uri("/v1/auth/signup")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			).hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.roles")
				.isEqualTo(listOf("ROOT_ADMIN"))
		}

		@Test
		@DisplayName("ROOT_ADMIN이 아닌 어드민이 ROOT_ADMIN 역할을 부여하려 하면 실패한다")
		fun nonRootAdminCannotAssignRootAdminRole() {
			// given
			val admin = adminUserTestFactory.persistPartnerAdmin()
			val accessToken = getAccessToken(admin)

			val request =
				mapOf(
					"email" to "root-attempt@bottlenote.com",
					"password" to "password123",
					"name" to "루트 시도",
					"roles" to listOf("ROOT_ADMIN")
				)

			// when & then
			assertThat(
				mockMvcTester
					.post()
					.uri("/v1/auth/signup")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			).hasStatus4xxClientError()
				.bodyJson()
				.extractingPath("$.success")
				.isEqualTo(false)
		}

		@Test
		@DisplayName("인증되지 않은 사용자는 회원가입 API를 호출할 수 없다")
		fun signupFailWithoutAuth() {
			// given
			val request =
				mapOf(
					"email" to "no-auth@bottlenote.com",
					"password" to "password123",
					"name" to "인증 없는 사용자",
					"roles" to listOf("PARTNER")
				)

			// when & then
			assertThat(
				mockMvcTester
					.post()
					.uri("/v1/auth/signup")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			).hasStatus4xxClientError()
		}

		@Test
		@DisplayName("중복된 이메일로 회원가입 시 실패한다")
		fun signupFailWithDuplicateEmail() {
			// given
			val existingEmail = "existing@bottlenote.com"
			adminUserTestFactory.persistAdminUser(existingEmail, "password123", "기존 어드민", listOf(AdminRole.PARTNER))

			val admin = adminUserTestFactory.persistRootAdmin()
			val accessToken = getAccessToken(admin)

			val request =
				mapOf(
					"email" to existingEmail,
					"password" to "password123",
					"name" to "중복 시도",
					"roles" to listOf("PARTNER")
				)

			// when & then
			assertThat(
				mockMvcTester
					.post()
					.uri("/v1/auth/signup")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			).hasStatus4xxClientError()
				.bodyJson()
				.extractingPath("$.success")
				.isEqualTo(false)
		}

		@Test
		@DisplayName("역할이 비어있으면 회원가입 시 실패한다")
		fun signupFailWithEmptyRoles() {
			// given
			val admin = adminUserTestFactory.persistRootAdmin()
			val accessToken = getAccessToken(admin)

			val request =
				mapOf(
					"email" to "empty-roles@bottlenote.com",
					"password" to "password123",
					"name" to "역할 없음",
					"roles" to emptyList<String>()
				)

			// when & then
			assertThat(
				mockMvcTester
					.post()
					.uri("/v1/auth/signup")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			).hasStatus4xxClientError()
		}
	}
}
