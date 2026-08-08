package app.global.security

import app.IntegrationTestSupport
import app.bottlenote.global.security.accesscontrol.AccessControlService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import java.time.Duration
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/**
 * access-control을 이 클래스에서만 활성화하고, 실제 Redis + Security filter chain을 검증한다.
 * application-test.yml 기본값(enabled=false)은 다른 통합 테스트에 유지된다.
 */
@Tag("admin_integration")
@DisplayName("[integration] Admin AccessControl Security chain")
@ActiveProfiles("test", "access-control-it")
class AccessControlSecurityIntegrationTest : IntegrationTestSupport() {
	companion object {
		private const val PROTECTED_PATH = "/v1/users"
		private const val BAN_API = "/v1/access-control/ip-bans"
		private val IP_SEQ = AtomicInteger(0)
	}

	@Autowired
	private lateinit var accessControlService: AccessControlService

	private lateinit var accessToken: String

	@BeforeEach
	fun setUpAdminToken() {
		val admin = adminUserTestFactory.persistRootAdmin()
		accessToken = getAccessToken(admin)
	}

	@Test
	@DisplayName("허용 IP 요청은 Security chain을 통과한다")
	fun allowedIpPassesFilterChain() {
		val clientIp = nextTestIp()

		val result =
			mockMvcTester
				.get()
				.uri(PROTECTED_PATH)
				.header("Authorization", "Bearer $accessToken")
				.header("X-Forwarded-For", clientIp)
				.exchange()

		result.assertThat().hasStatusOk()
		assertThat(result.response.getHeader("X-RateLimit-Limit")).isEqualTo("3")
	}

	@Test
	@DisplayName("ban된 IP 요청은 403을 반환한다")
	fun bannedIpReturns403() {
		val clientIp = nextTestIp()
		accessControlService.banIp(clientIp, Duration.ofMinutes(5), "integration-ban")
		try {
			val result =
				mockMvcTester
					.get()
					.uri(PROTECTED_PATH)
					.header("Authorization", "Bearer $accessToken")
					.header("X-Forwarded-For", clientIp)
					.exchange()

			result.assertThat().hasStatus(HttpStatus.FORBIDDEN)
			assertThat(result.response.getHeader("Retry-After")).isNotBlank()
		} finally {
			accessControlService.unbanIp(clientIp)
		}
	}

	@Test
	@DisplayName("default rate limit을 초과하면 429를 반환한다")
	fun rateLimitExceededReturns429() {
		val clientIp = nextTestIp()

		repeat(3) {
			mockMvcTester
				.get()
				.uri(PROTECTED_PATH)
				.header("Authorization", "Bearer $accessToken")
				.header("X-Forwarded-For", clientIp)
				.exchange()
				.assertThat()
				.hasStatusOk()
		}

		val limited =
			mockMvcTester
				.get()
				.uri(PROTECTED_PATH)
				.header("Authorization", "Bearer $accessToken")
				.header("X-Forwarded-For", clientIp)
				.exchange()

		limited.assertThat().hasStatus(HttpStatus.TOO_MANY_REQUESTS)
		assertThat(limited.response.getHeader("Retry-After")).isNotBlank()
		assertThat(limited.response.getHeader("X-RateLimit-Remaining")).isEqualTo("0")
	}

	@Test
	@DisplayName("관리 API는 인증 없이 호출할 수 없다")
	fun managementApiRequiresAuthentication() {
		val targetIp = nextTestIp()
		val banBody =
			mapper.writeValueAsString(
				mapOf(
					"ip" to targetIp,
					"ttlSeconds" to 60,
					"reason" to "auth-check"
				)
			)

		// admin SecurityConfig는 AuthenticationEntryPoint 미설정 → 무토큰 시 403 Access Denied
		mockMvcTester
			.post()
			.uri(BAN_API)
			.contentType(MediaType.APPLICATION_JSON)
			.content(banBody)
			.exchange()
			.assertThat()
			.hasStatus(HttpStatus.FORBIDDEN)

		mockMvcTester
			.get()
			.uri(BAN_API)
			.exchange()
			.assertThat()
			.hasStatus(HttpStatus.FORBIDDEN)

		mockMvcTester
			.delete()
			.uri("$BAN_API?ip=$targetIp")
			.exchange()
			.assertThat()
			.hasStatus(HttpStatus.FORBIDDEN)
	}

	@Test
	@DisplayName("banned IP도 관리 API unban으로 탈출할 수 있다")
	fun bannedIpCanEscapeViaManagementUnban() {
		val escapeIp = nextTestIp()
		val banBody =
			mapper.writeValueAsString(
				mapOf(
					"ip" to escapeIp,
					"ttlSeconds" to 600,
					"reason" to "escape-path"
				)
			)

		// given: 관리 API로 ban
		mockMvcTester
			.post()
			.uri(BAN_API)
			.header("Authorization", "Bearer $accessToken")
			.contentType(MediaType.APPLICATION_JSON)
			.content(banBody)
			.exchange()
			.assertThat()
			.hasStatusOk()

		try {
			// when: ban된 IP로 일반 API 호출 → 403
			mockMvcTester
				.get()
				.uri(PROTECTED_PATH)
				.header("Authorization", "Bearer $accessToken")
				.header("X-Forwarded-For", escapeIp)
				.exchange()
				.assertThat()
				.hasStatus(HttpStatus.FORBIDDEN)

			// when: 동일 banned IP로 unban 관리 API 호출 → 통과 (management path)
			mockMvcTester
				.delete()
				.uri("$BAN_API?ip=$escapeIp")
				.header("Authorization", "Bearer $accessToken")
				.header("X-Forwarded-For", escapeIp)
				.exchange()
				.assertThat()
				.hasStatusOk()

			// then: unban 후 일반 API 재개
			mockMvcTester
				.get()
				.uri(PROTECTED_PATH)
				.header("Authorization", "Bearer $accessToken")
				.header("X-Forwarded-For", escapeIp)
				.exchange()
				.assertThat()
				.hasStatusOk()
		} finally {
			accessControlService.unbanIp(escapeIp)
		}
	}

	/**
	 * RFC 2544 벤치마크 대역(198.18.0.0/15)에서 실행마다 고유 IP를 만든다.
	 * Redis rate-limit 키 충돌을 피하기 위해 nanoTime·UUID·시퀀스를 조합한다.
	 */
	private fun nextTestIp(): String {
		val uuid = UUID.randomUUID()
		val seq = IP_SEQ.incrementAndGet()
		val b = Math.floorMod((uuid.mostSignificantBits xor System.nanoTime()).toInt(), 2) + 18
		val c = Math.floorMod((uuid.leastSignificantBits xor seq.toLong()).toInt(), 254) + 1
		val d = Math.floorMod((uuid.hashCode() xor seq * 31), 254) + 1
		return "198.$b.$c.$d"
	}
}
