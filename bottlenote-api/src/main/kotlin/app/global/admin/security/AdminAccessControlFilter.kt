package app.global.admin.security

import app.bottlenote.global.security.accesscontrol.AccessControlFilter
import app.global.admin.AdminApi
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter

// admin 체인 전용 IP ban·rate limit 필터. 기존 admin 앱 설정으로 만든 AccessControlFilter에 판정을 맡긴다.
// 기존 admin 앱은 context-path(/admin/api)를 뺀 경로로 규칙을 골랐다. 위임 필터에는 mount를 뺀 경로를 보여 주고,
// 다음 필터에는 원래 요청을 넘긴다. 인코딩을 바꾼 mount(/admin/%61pi)도 같은 규칙과 버킷을 쓰게 하기 위해서다.
class AdminAccessControlFilter(
	private val delegate: AccessControlFilter
) : OncePerRequestFilter() {
	override fun doFilterInternal(
		request: HttpServletRequest,
		response: HttpServletResponse,
		filterChain: FilterChain
	) {
		var allowed = false
		delegate.doFilter(MountStrippedRequest(request), response, FilterChain { _, _ -> allowed = true })
		if (allowed) {
			filterChain.doFilter(request, response)
		}
	}

	// AccessControlFilter는 requestURI에서 contextPath를 빼서 경로를 구한다. 이 두 값만 admin 앱 기준으로 바꾼다.
	private class MountStrippedRequest(
		request: HttpServletRequest
	) : HttpServletRequestWrapper(request) {
		private val pathWithinMount = AdminApi.pathWithinMount(AdminApi.pathWithinApplication(request.requestURI, request.contextPath))

		override fun getRequestURI(): String = pathWithinMount ?: super.getRequestURI()

		override fun getContextPath(): String = if (pathWithinMount != null) "" else super.getContextPath()
	}
}
