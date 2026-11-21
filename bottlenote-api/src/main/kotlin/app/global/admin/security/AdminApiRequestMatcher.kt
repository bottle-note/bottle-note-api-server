package app.global.admin.security

import app.global.admin.AdminApi
import jakarta.servlet.DispatcherType
import jakarta.servlet.RequestDispatcher
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.web.util.matcher.RequestMatcher

// admin 보안 체인을 고르는 matcher.
// ERROR dispatch는 요청 경로가 /error로 바뀌므로 원래 요청 URI로 판정한다.
// 그래야 admin 인증 실패가 sendError로 넘어간 뒤에도 admin 앱과 같은 체인 설정에서 응답이 만들어진다.
object AdminApiRequestMatcher : RequestMatcher {
	override fun matches(request: HttpServletRequest): Boolean {
		if (request.dispatcherType == DispatcherType.ERROR) {
			val originalUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI) as? String ?: return false
			return AdminApi.isAdminApiPath(AdminApi.pathWithinApplication(originalUri, request.contextPath))
		}
		return AdminApi.isAdminApiRequest(request)
	}

	override fun toString(): String = "AdminApiRequestMatcher[${AdminApi.PATH_PREFIX}/**]"
}
