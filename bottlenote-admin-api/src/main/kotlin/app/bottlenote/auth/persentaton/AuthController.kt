package app.bottlenote.auth.persentaton

import app.bottlenote.auth.config.RootAdminProperties
import app.bottlenote.global.data.response.GlobalResponse
import app.bottlenote.user.dto.response.TokenItem
import app.bottlenote.user.service.AdminAuthService
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

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
		//다만 최초 슈퍼어드민이 없는 경우 초기화 요청하는 로직 구현
		val rootAdminIsAlive = authService.rootAdminIsAlive();
		log.info("루트 어드민 존재 여부. {}", rootAdminIsAlive)
		if (!rootAdminIsAlive) {
			log.info("루트 어드민 초기 생성 로직 호출")
			val email = rootAdminProperties.email
			val encodedPassword = rootAdminProperties.getEncodedPassword(encoder)
			log.info("email: {},encodedPassword: {}", email, encodedPassword)
			authService.initRootAdmin(email, encodedPassword)
		}
	}

	@PostMapping("/login")
	fun login(
		username: String,
		password: String
	): ResponseEntity<*> {
		val tokenItem: TokenItem = authService.login(username, encoder.encode(password))
		return GlobalResponse.ok(tokenItem)
	}
}
