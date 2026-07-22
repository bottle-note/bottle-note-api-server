package app.global.security

import app.IntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.test.web.servlet.assertj.MvcTestResult

@Tag("admin_integration")
@DisplayName("[integration] Admin SecurityConfig CORS 테스트")
class SecurityConfigIntegrationTest : IntegrationTestSupport() {
	@Test
	@DisplayName("Admin Origin의 preflight 요청에 해당 Origin을 반환한다")
	fun allowedAdminOriginPreflight() {
		val result = preflight("https://admin.bottle-note.com")

		result.assertThat().hasStatusOk()
		assertThat(result.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
			.isEqualTo("https://admin.bottle-note.com")
	}

	@Test
	@DisplayName("Product Origin의 Admin API preflight 요청은 거부한다")
	fun productOriginPreflightRejected() {
		val result = preflight("https://bottle-note.com")

		result.assertThat().hasStatus(FORBIDDEN)
		assertThat(result.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull()
	}

	@Test
	@DisplayName("임의 Origin의 Admin API preflight 요청은 거부한다")
	fun arbitraryOriginPreflightRejected() {
		val result = preflight("https://evil.example")

		result.assertThat().hasStatus(FORBIDDEN)
		assertThat(result.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull()
	}

	private fun preflight(origin: String): MvcTestResult = mockMvcTester
		.options()
		.uri("/v1/users")
		.header(HttpHeaders.ORIGIN, origin)
		.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
		.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type")
		.exchange()
}
