package app

import app.global.admin.AdminApi
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.server.PathContainer
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.test.web.servlet.request.RequestPostProcessor
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcConfigurer
import org.springframework.web.context.WebApplicationContext

// admin 앱은 context-path(/admin/api) 아래에서 /v1/... 경로를 받았고, admin 테스트는 그 안쪽 경로로 요청을 만든다.
// 통합 앱에는 context-path가 없고 같은 외부 URL을 /admin/api/v1/... 핸들러가 받으므로, 모든 MockMvc 요청 앞에 mount를 한곳에서 붙인다.
@TestConfiguration(proxyBeanMethods = false)
class AdminApiMountTestConfig {
	@Bean
	fun adminApiMountMockMvcCustomizer(): MockMvcBuilderCustomizer = MockMvcBuilderCustomizer { builder ->
		builder.apply<ConfigurableMockMvcBuilder<*>>(AdminApiMountConfigurer)
	}

	private object AdminApiMountConfigurer : MockMvcConfigurer {
		override fun beforeMockMvcCreated(
			builder: ConfigurableMockMvcBuilder<*>,
			context: WebApplicationContext
		): RequestPostProcessor = RequestPostProcessor { request -> mount(request) }
	}

	companion object {
		private val MOUNTED = AdminApiMountTestConfig::class.java.name + ".MOUNTED"

		// admin 앱의 context-path 안쪽에서 첫 세그먼트로 오던 값. swagger-ui는 비활성화 여부를 확인하는 요청이 쓴다.
		private val ADMIN_APP_SEGMENTS = setOf("v1", "v2", "swagger-ui")

		private const val DEFAULT_SCHEME = "http"
		private const val DEFAULT_HOST = "localhost"
		private const val DEFAULT_PORT = 80

		/**
		 * admin 앱 context-path(/admin/api) 안쪽의 상대 경로를 통합 앱의 외부 경로로 옮긴다.
		 *
		 * 받는 입력은 /v1/..., /v2/..., admin 문서 경로(/v1/openapi.admin.json), 비활성화된 springdoc UI 경로(/swagger-ui/...)뿐이다.
		 * 다음 입력은 테스트가 의도와 다른 경로를 검증하게 되므로 IllegalArgumentException으로 바로 실패시킨다.
		 * - 이미 mount된 경로와 mount를 인코딩한 경로(/admin/..., /%61dmin/...)
		 * - product 경로(/api/...)와 통합 앱 루트의 경로(/error, /actuator/... 등)
		 * - 기본 호스트(http://localhost:80)가 아닌 전체 URL. 기본 호스트의 전체 URL은 MockMvc가 경로만 쓰므로 상대 경로와 같게 다룬다.
		 * 요청 URI와 pathInfo를 함께 옮겨 Security matcher와 MVC가 같은 경로를 보게 한다. 인코딩은 원문 그대로 둔다.
		 * 같은 request builder를 다시 perform하면 기본 요청의 post-processor가 한 번 더 합쳐지므로, 요청마다 한 번만 붙인다.
		 */
		fun mount(request: MockHttpServletRequest): MockHttpServletRequest {
			if (request.getAttribute(MOUNTED) != null) {
				return request
			}
			requireRelativeAdminPath(request)
			request.setAttribute(MOUNTED, true)
			request.requestURI = AdminApi.PATH_PREFIX + request.requestURI
			request.pathInfo?.let { request.pathInfo = AdminApi.PATH_PREFIX + it }
			return request
		}

		private fun requireRelativeAdminPath(request: MockHttpServletRequest) {
			val path = request.requestURI
			require(request.scheme == DEFAULT_SCHEME && request.serverName == DEFAULT_HOST && request.serverPort == DEFAULT_PORT) {
				"admin 테스트는 기본 호스트가 아닌 전체 URL로 요청할 수 없다: ${request.requestURL}"
			}
			when (val first = firstSegment(path)) {
				"admin" -> throw IllegalArgumentException("이미 mount된 경로이거나 mount를 인코딩한 경로다: $path. admin 테스트는 /v1/...처럼 mount 안쪽 경로로 요청한다")
				"api" -> throw IllegalArgumentException("product 경로는 admin 테스트의 mount 보정 대상이 아니다: $path")
				!in ADMIN_APP_SEGMENTS -> throw IllegalArgumentException("admin 테스트의 요청 경로는 $ADMIN_APP_SEGMENTS 중 하나로 시작해야 한다: $path (첫 세그먼트 '$first')")
			}
		}

		// MVC와 같은 기준(세그먼트마다 한 번 디코딩)으로 첫 세그먼트를 읽는다
		private fun firstSegment(path: String): String {
			val parsed =
				try {
					PathContainer.parsePath(path)
				} catch (e: IllegalArgumentException) {
					throw IllegalArgumentException("admin 테스트의 요청 경로를 해석할 수 없다: $path", e)
				}
			return parsed.elements().filterIsInstance<PathContainer.PathSegment>().firstOrNull()?.valueToMatch() ?: ""
		}
	}
}
