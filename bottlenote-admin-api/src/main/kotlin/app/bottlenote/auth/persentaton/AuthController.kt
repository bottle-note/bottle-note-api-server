package app.bottlenote.auth.persentaton

import app.bottlenote.auth.config.RootAdminProperties
import app.bottlenote.global.data.response.GlobalResponse
import app.bottlenote.global.security.SecurityContextUtil
import app.bottlenote.user.dto.response.TokenItem
import app.bottlenote.user.exception.UserException
import app.bottlenote.user.exception.UserExceptionCode
import app.bottlenote.user.service.AdminAuthService
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/auth")
class AuthController(
	private val authService: AdminAuthService,
	private val rootAdminProperties: RootAdminProperties,
	private val encoder: BCryptPasswordEncoder
) {
	private val log = LoggerFactory.getLogger(javaClass)

	@EventListener(ApplicationReadyEvent::class)
	fun onApplicationReady() {
		val rootAdminIsAlive = authService.rootAdminIsAlive()
		log.info("루트 어드민 존재 여부: {}", rootAdminIsAlive)
		if (!rootAdminIsAlive) {
			log.info("루트 어드민 초기 생성 로직 호출")
			val email = rootAdminProperties.email
			val encodedPassword = rootAdminProperties.getEncodedPassword(encoder)
			authService.initRootAdmin(email, encodedPassword)
		}
	}

	@PostMapping("/login")
	fun login(@RequestBody request: LoginRequest): ResponseEntity<*> {
		val tokenItem: TokenItem = authService.login(request.email, request.password)
		return GlobalResponse.ok(tokenItem)
	}

	@PostMapping("/refresh")
	fun refresh(@RequestBody request: RefreshRequest): ResponseEntity<*> {
		val tokenItem: TokenItem = authService.refresh(request.refreshToken)
		return GlobalResponse.ok(tokenItem)
	}

	@DeleteMapping("/withdraw")
	fun withdraw(): ResponseEntity<*> {
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
