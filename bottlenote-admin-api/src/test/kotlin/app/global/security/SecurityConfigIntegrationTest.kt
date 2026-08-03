package app.global.security

import app.IntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.test.web.servlet.assertj.MvcTestResult

@Tag("admin_integration")
@DisplayName("[integration] Admin SecurityConfig CORS 테스트")
class SecurityConfigIntegrationTest : IntegrationTestSupport() {
	@Value("\${springdoc.api-docs.path}")
	private lateinit var openApiSpecPath: String

	@Test
	@DisplayName("Admin Origin의 preflight 요청에 해당 Origin을 반환한다")
	fun allowedAdminOriginPreflight() {
		val result = preflight("https://admin.bottle-note.com")

		result.assertThat().hasStatusOk()
		assertThat(result.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
			.isEqualTo("https://admin.bottle-note.com")
	}

	@Test
	@DisplayName("GitHub Pages Origin의 일반 Admin API preflight 요청을 허용한다")
	fun githubPagesOriginAdminApiPreflightAllowed() {
		val result = preflight("https://bottle-note.github.io")

		result.assertThat().hasStatusOk()
		assertThat(result.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
			.isEqualTo("https://bottle-note.github.io")
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

	@Test
	@DisplayName("문서 스펙 경로는 일반 API에서 허용된 Origin이어도 403이고 permissive CORS 헤더를 반환하지 않는다")
	fun openApiSpecOriginRequestRejected() {
		val result = mockMvcTester
			.get()
			.uri(openApiSpecPath)
			.header(HttpHeaders.ORIGIN, "https://admin.bottle-note.com")
			.exchange()

		result.assertThat().hasStatus(FORBIDDEN)
		assertThat(result.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull()
	}

	@Test
	@DisplayName("문서 스펙 경로의 preflight 요청도 일반 API에서 허용된 Origin이어도 403으로 거부한다")
	fun openApiSpecPreflightRejected() {
		val result = mockMvcTester
			.options()
			.uri(openApiSpecPath)
			.header(HttpHeaders.ORIGIN, "https://admin.bottle-note.com")
			.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
			.exchange()

		result.assertThat().hasStatus(FORBIDDEN)
		assertThat(result.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull()
	}

	@Test
	@DisplayName("GitHub Pages Origin의 문서 스펙 요청을 허용한다")
	fun openApiSpecGitHubPagesOriginAllowed() {
		val result = mockMvcTester
			.get()
			.uri(openApiSpecPath)
			.header(HttpHeaders.ORIGIN, "https://bottle-note.github.io")
			.exchange()

		result.assertThat().hasStatusOk()
		assertThat(result.response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
			.isEqualTo("https://bottle-note.github.io")
	}

	@Test
	@DisplayName("문서 스펙 경로는 Origin이 없는 무인증 GET을 정상 허용한다")
	fun openApiSpecNoOriginRequestAllowed() {
		val result = mockMvcTester
			.get()
			.uri(openApiSpecPath)
			.exchange()

		result.assertThat().hasStatusOk()
	}
}
