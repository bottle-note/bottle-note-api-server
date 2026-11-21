package app.global.admin

import app.bottlenote.curation.presentation.AdminCurationSpecController
import app.bottlenote.curation.presentation.AdminSpecBasedCurationController
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.server.PathContainer
import org.springframework.web.util.pattern.PathPatternParser

// product 앱이 흡수한 admin API의 경계를 한곳에 모은다.
// 경로 prefix, 보안 체인 선택, 문서 분리, 예외 처리 범위가 모두 이 정의를 기준으로 한다.
object AdminApi {
	// admin 앱의 context-path였던 경로. 외부 URL을 바꾸지 않으려고 그대로 쓴다.
	const val PATH_PREFIX = "/admin/api"

	// admin 앱이 /v1을 붙이던 컨트롤러의 최종 prefix
	const val LEGACY_PATH_PREFIX = "$PATH_PREFIX/v1"

	// admin 앱의 springdoc.api-docs.path(/v1/openapi.admin.json)를 context-path와 합친 경로
	const val DOCS_PATH = "$PATH_PREFIX/v1/openapi.admin.json"

	// 이식한 admin 컨트롤러가 놓인 패키지. 이름 규칙이 아니라 이 목록으로 admin 컨트롤러를 판정한다.
	const val ACCESS_CONTROL_PACKAGE = "app.bottlenote.accesscontrol.presentation"
	const val ALCOHOLS_PACKAGE = "app.bottlenote.alcohols.presentation"
	const val AUTH_PACKAGE = "app.bottlenote.auth.presentation"
	const val BANNER_PACKAGE = "app.bottlenote.banner.presentation"
	const val CAMPAIGN_CONTENT_PACKAGE = "app.bottlenote.campaigncontent.presentation"
	const val FILE_PACKAGE = "app.bottlenote.common.file.presentation"
	const val CURATION_PACKAGE = "app.bottlenote.curation.presentation"
	const val MFDS_PACKAGE = "app.bottlenote.mfds.presentation"
	const val REVIEW_PACKAGE = "app.bottlenote.review.presentation"
	const val STATISTICS_PACKAGE = "app.bottlenote.statistics.presentation"
	const val HELP_PACKAGE = "app.bottlenote.support.help.presentation"
	const val USER_PACKAGE = "app.bottlenote.user.presentation"

	val CONTROLLER_PACKAGES: List<String> = listOf(
		ACCESS_CONTROL_PACKAGE,
		ALCOHOLS_PACKAGE,
		AUTH_PACKAGE,
		BANNER_PACKAGE,
		CAMPAIGN_CONTENT_PACKAGE,
		FILE_PACKAGE,
		CURATION_PACKAGE,
		MFDS_PACKAGE,
		REVIEW_PACKAGE,
		STATISTICS_PACKAGE,
		HELP_PACKAGE,
		USER_PACKAGE
	)

	// admin 앱에서도 /v1을 붙이지 않고 자체 매핑(/v2/...)을 쓰던 컨트롤러
	val V2_CONTROLLERS: Set<Class<*>> = setOf(
		AdminCurationSpecController::class.java,
		AdminSpecBasedCurationController::class.java
	)

	private val ADMIN_API_PATTERN = PathPatternParser.defaultInstance.parse("$PATH_PREFIX/**")

	private val MOUNT_ELEMENT_COUNT = PathContainer.parsePath(PATH_PREFIX).elements().size

	fun isAdminController(type: Class<*>): Boolean {
		val packageName = type.packageName
		return CONTROLLER_PACKAGES.any { packageName == it || packageName.startsWith("$it.") }
	}

	fun isLegacyController(type: Class<*>): Boolean = isAdminController(type) && type !in V2_CONTROLLERS

	fun isV2Controller(type: Class<*>): Boolean = type in V2_CONTROLLERS

	// MVC 라우팅과 같은 기준(context-path를 뺀 뒤 세그먼트별로 디코딩)으로 판정한다.
	// raw URI 문자열 비교를 쓰면 %61dmin 같은 인코딩 변형이 admin 경계를 벗어난다.
	// 디코딩할 수 없는 경로는 admin 쪽으로 판정한다. 더 엄격한 admin 체인이 처리하도록 하기 위해서다.
	fun isAdminApiPath(pathWithinApplication: String): Boolean = try {
		ADMIN_API_PATTERN.matches(PathContainer.parsePath(pathWithinApplication.ifBlank { "/" }))
	} catch (e: IllegalArgumentException) {
		true
	}

	fun isAdminApiRequest(request: HttpServletRequest): Boolean = isAdminApiPath(pathWithinApplication(request.requestURI, request.contextPath))

	// 기존 admin 앱이 보던 경로(mount를 뺀 안쪽 경로)를 MVC와 같은 세그먼트 해석으로 돌려준다. admin 경로가 아니거나 해석할 수 없으면 null이다.
	// 세그먼트마다 한 번 디코딩한 값(valueToMatch)을 잇는다. StrictHttpFirewall이 %2F, %25, %2E, 세미콜론, 비정규화 경로를
	// 보안 체인 선택 전에 거부하므로, 디코딩한 값을 이어도 세그먼트 경계는 MVC가 본 것과 같다.
	fun pathWithinMount(pathWithinApplication: String): String? {
		val path = try {
			PathContainer.parsePath(pathWithinApplication.ifBlank { "/" })
		} catch (e: IllegalArgumentException) {
			return null
		}
		if (!ADMIN_API_PATTERN.matches(path)) {
			return null
		}
		val inner = path.elements().drop(MOUNT_ELEMENT_COUNT)
		if (inner.isEmpty()) {
			return "/"
		}
		return inner.joinToString("") { element ->
			if (element is PathContainer.PathSegment) element.valueToMatch() else element.value()
		}
	}

	fun pathWithinApplication(requestUri: String?, contextPath: String?): String {
		val uri = requestUri ?: return "/"
		if (!contextPath.isNullOrEmpty() && uri.startsWith(contextPath)) {
			return uri.substring(contextPath.length)
		}
		return uri
	}
}
