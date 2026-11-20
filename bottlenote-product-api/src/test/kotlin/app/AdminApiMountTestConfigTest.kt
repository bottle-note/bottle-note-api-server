package app

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockServletContext
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.RequestPostProcessor
import java.net.URI

@Tag("unit")
@DisplayName("[unit] admin 테스트의 mount 보정")
class AdminApiMountTestConfigTest {
	private val servletContext = MockServletContext()

	@ParameterizedTest
	@ValueSource(strings = ["/v1/users", "/v2/curation-specs", "/v1/openapi.admin.json", "/swagger-ui/index.html"])
	@DisplayName("admin 앱 안쪽의 상대 경로를 받을 때 요청 URI와 pathInfo 앞에 /admin/api를 붙인다")
	fun mountsRelativeAdminPath(path: String) {
		val request = AdminApiMountTestConfig.mount(build(path))

		assertThat(request.requestURI).isEqualTo("/admin/api$path")
		assertThat(request.pathInfo).isEqualTo("/admin/api$path")
	}

	@Test
	@DisplayName("인코딩한 안쪽 경로를 받을 때 요청 URI의 원문 인코딩을 그대로 둔다")
	fun keepsRawEncodingOfInnerPath() {
		val request = AdminApiMountTestConfig.mount(build(URI.create("/v1/%75sers")))

		assertThat(request.requestURI).isEqualTo("/admin/api/v1/%75sers")
		assertThat(request.pathInfo).isEqualTo("/admin/api/v1/users")
	}

	@Test
	@DisplayName("기본 호스트의 전체 URL을 받을 때 MockMvc가 쓰는 경로만 보고 상대 경로와 같게 붙인다")
	fun treatsDefaultHostUrlAsRelativePath() {
		val request = AdminApiMountTestConfig.mount(build("http://localhost/v1/users"))

		assertThat(request.requestURI).isEqualTo("/admin/api/v1/users")
	}

	@ParameterizedTest
	@ValueSource(strings = ["/admin/api/v1/users", "/admin/%61pi/v1/users", "/%61dmin/api/v1/users"])
	@DisplayName("이미 mount된 경로나 mount를 인코딩한 경로를 받을 때 바로 실패한다")
	fun rejectsMountedOrEncodedMountPath(path: String) {
		assertThatThrownBy { AdminApiMountTestConfig.mount(build(URI.create(path))) }
			.isInstanceOf(IllegalArgumentException::class.java)
			.hasMessageContaining("mount")
	}

	@Test
	@DisplayName("product 경로를 받을 때 바로 실패한다")
	fun rejectsProductPath() {
		assertThatThrownBy { AdminApiMountTestConfig.mount(build("/api/v1/alcohols")) }
			.isInstanceOf(IllegalArgumentException::class.java)
			.hasMessageContaining("product")
	}

	@ParameterizedTest
	@ValueSource(strings = ["/error", "/actuator/health", "/", "/favicon.ico"])
	@DisplayName("통합 앱 루트의 경로처럼 admin 앱 안쪽 경로가 아닌 값을 받을 때 바로 실패한다")
	fun rejectsPathOutsideAdminApp(path: String) {
		assertThatThrownBy { AdminApiMountTestConfig.mount(build(path)) }
			.isInstanceOf(IllegalArgumentException::class.java)
			.hasMessageContaining("요청 경로")
	}

	@ParameterizedTest
	@ValueSource(strings = ["http://admin.bottle-note.com/v1/users", "http://localhost:8080/v1/users", "https://localhost/v1/users"])
	@DisplayName("기본 호스트가 아닌 전체 URL을 받을 때 바로 실패한다")
	fun rejectsAbsoluteUrlOfOtherHost(url: String) {
		assertThatThrownBy { AdminApiMountTestConfig.mount(build(url)) }
			.isInstanceOf(IllegalArgumentException::class.java)
			.hasMessageContaining("전체 URL")
	}

	@Test
	@DisplayName("같은 요청에 보정을 두 번 적용할 때 /admin/api를 한 번만 붙인다")
	fun mountsOnlyOncePerRequest() {
		val request = build("/v1/users")

		AdminApiMountTestConfig.mount(request)
		AdminApiMountTestConfig.mount(request)

		assertThat(request.requestURI).isEqualTo("/admin/api/v1/users")
		assertThat(request.pathInfo).isEqualTo("/admin/api/v1/users")
	}

	@Test
	@DisplayName("기본 요청이 다시 합쳐져 post-processor가 두 번 등록될 때도 한 번만 붙인다")
	fun mountsOnlyOnceWhenPostProcessorIsMergedTwice() {
		val mount = RequestPostProcessor { AdminApiMountTestConfig.mount(it) }
		val builder = MockMvcRequestBuilders.get("/v1/users").with(mount).with(mount)

		val request = builder.postProcessRequest(builder.buildRequest(servletContext))

		assertThat(request.requestURI).isEqualTo("/admin/api/v1/users")
	}

	private fun build(uri: String): MockHttpServletRequest = MockMvcRequestBuilders.get(uri).buildRequest(servletContext)

	private fun build(uri: URI): MockHttpServletRequest = MockMvcRequestBuilders.get(uri).buildRequest(servletContext)
}
