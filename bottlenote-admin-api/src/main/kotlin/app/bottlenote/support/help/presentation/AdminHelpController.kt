package app.bottlenote.support.help.presentation

import app.bottlenote.global.data.response.GlobalResponse
import app.bottlenote.global.security.SecurityContextUtil
import app.bottlenote.global.service.meta.MetaService
import app.bottlenote.support.help.dto.request.AdminHelpAnswerRequest
import app.bottlenote.support.help.dto.request.AdminHelpPageableRequest
import app.bottlenote.support.help.presentation.docs.AdminHelpApiDocs
import app.bottlenote.support.help.service.AdminHelpService
import app.bottlenote.user.exception.UserException
import app.bottlenote.user.exception.UserExceptionCode
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/helps")
@AdminHelpApiDocs.ApiTag
class AdminHelpController(
	private val adminHelpService: AdminHelpService
) {
	@AdminHelpApiDocs.GetHelpList
	@GetMapping
	fun getHelpList(
		@ModelAttribute request: AdminHelpPageableRequest
	): ResponseEntity<GlobalResponse> {
		val page = adminHelpService.getHelpList(request)
		return GlobalResponse.ok(
			page.content(),
			MetaService.createMetaInfo().add("pagination", page.pagination())
		)
	}

	@AdminHelpApiDocs.GetHelpDetail
	@GetMapping("/{helpId}")
	fun getHelpDetail(
		@PathVariable helpId: Long
	): ResponseEntity<GlobalResponse> {
		val response = adminHelpService.getHelpDetail(helpId)
		return GlobalResponse.ok(response)
	}

	@AdminHelpApiDocs.AnswerHelp
	@PostMapping("/{helpId}/answer")
	fun answerHelp(
		@PathVariable helpId: Long,
		@RequestBody @Valid request: AdminHelpAnswerRequest
	): ResponseEntity<GlobalResponse> {
		val adminId =
			SecurityContextUtil
				.getAdminUserIdByContext()
				.orElseThrow { UserException(UserExceptionCode.REQUIRED_USER_ID) }
		val response = adminHelpService.answerHelp(helpId, adminId, request)
		return GlobalResponse.ok(response)
	}
}
