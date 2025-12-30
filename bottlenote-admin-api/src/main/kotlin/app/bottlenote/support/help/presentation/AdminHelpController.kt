package app.bottlenote.support.help.presentation

import app.bottlenote.global.data.response.GlobalResponse
import app.bottlenote.global.security.SecurityContextUtil
import app.bottlenote.support.help.dto.request.AdminHelpAnswerRequest
import app.bottlenote.support.help.dto.request.AdminHelpPageableRequest
import app.bottlenote.support.help.service.AdminHelpService
import app.bottlenote.user.exception.UserException
import app.bottlenote.user.exception.UserExceptionCode
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/helps")
class AdminHelpController(
	private val adminHelpService: AdminHelpService
) {

	@GetMapping
	fun getHelpList(@ModelAttribute request: AdminHelpPageableRequest): ResponseEntity<*> {
		val response = adminHelpService.getHelpList(request)
		return GlobalResponse.ok(response)
	}

	@GetMapping("/{helpId}")
	fun getHelpDetail(@PathVariable helpId: Long): ResponseEntity<*> {
		val response = adminHelpService.getHelpDetail(helpId)
		return GlobalResponse.ok(response)
	}

	@PostMapping("/{helpId}/answer")
	fun answerHelp(
		@PathVariable helpId: Long,
		@RequestBody @Valid request: AdminHelpAnswerRequest
	): ResponseEntity<*> {
		val adminId = SecurityContextUtil.getAdminUserIdByContext()
			.orElseThrow { UserException(UserExceptionCode.REQUIRED_USER_ID) }
		val response = adminHelpService.answerHelp(helpId, adminId, request)
		return GlobalResponse.ok(response)
	}
}
