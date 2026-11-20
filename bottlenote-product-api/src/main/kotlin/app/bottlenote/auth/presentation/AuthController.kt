package app.bottlenote.auth.presentation

import app.bottlenote.auth.presentation.docs.AuthApiDocs
import app.bottlenote.global.annotation.SecurityPolicy
import app.bottlenote.global.annotation.SecurityPolicy.AuthType.PUBLIC
import app.bottlenote.global.data.response.GlobalResponse
import app.bottlenote.global.security.SecurityContextUtil
import app.bottlenote.user.dto.request.AdminSignupRequest
import app.bottlenote.user.dto.request.AgentLoginRequest
import app.bottlenote.user.dto.response.TokenItem
import app.bottlenote.user.exception.UserException
import app.bottlenote.user.exception.UserExceptionCode
import app.bottlenote.user.service.AdminAuthService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

// 루트 관리자 초기화(onApplicationReady, RootAdminProperties)는 통합하면서 폐기했다.
@RestController
@RequestMapping("/auth")
@AuthApiDocs.ApiTag
class AuthController(
	private val authService: AdminAuthService
) {
	@AuthApiDocs.Login
	@SecurityPolicy(auth = PUBLIC)
	@PostMapping("/login")
	fun login(@RequestBody request: LoginRequest): ResponseEntity<GlobalResponse> {
		val tokenItem: TokenItem = authService.login(request.email, request.password)
		return GlobalResponse.ok(tokenItem)
	}

	@AuthApiDocs.Refresh
	@SecurityPolicy(auth = PUBLIC)
	@PostMapping("/refresh")
	fun refresh(@RequestBody request: RefreshRequest): ResponseEntity<GlobalResponse> {
		val tokenItem: TokenItem = authService.refresh(request.refreshToken)
		return GlobalResponse.ok(tokenItem)
	}

	@AuthApiDocs.LoginWithAgent
	@SecurityPolicy(auth = PUBLIC)
	@PostMapping("/agent")
	fun loginWithAgent(@RequestBody @Valid request: AgentLoginRequest): ResponseEntity<GlobalResponse> {
		val tokenItem: TokenItem = authService.loginWithAgent(request.agentKey())
		return GlobalResponse.ok(tokenItem)
	}

	@AuthApiDocs.Signup
	@PostMapping("/signup")
	fun signup(@RequestBody @Valid request: AdminSignupRequest): ResponseEntity<GlobalResponse> {
		val requesterId = SecurityContextUtil.getAdminUserIdByContext()
			.orElseThrow { UserException(UserExceptionCode.REQUIRED_USER_ID) }
		val response = authService.signup(requesterId, request)
		return GlobalResponse.ok(response)
	}

	@AuthApiDocs.Withdraw
	@DeleteMapping("/withdraw")
	fun withdraw(): ResponseEntity<GlobalResponse> {
		val adminId = SecurityContextUtil.getAdminUserIdByContext()
			.orElseThrow { UserException(UserExceptionCode.REQUIRED_USER_ID) }
		authService.withdraw(adminId)
		return GlobalResponse.ok(mapOf("message" to "탈퇴 처리되었습니다."))
	}

	data class LoginRequest(
		val email: String,
		val password: String
	)

	data class RefreshRequest(
		val refreshToken: String
	)
}
