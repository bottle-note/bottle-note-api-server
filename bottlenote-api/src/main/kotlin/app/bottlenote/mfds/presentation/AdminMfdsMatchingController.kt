package app.bottlenote.mfds.presentation

import app.bottlenote.global.data.response.GlobalResponse
import app.bottlenote.global.security.SecurityContextUtil
import app.bottlenote.mfds.dto.request.MfdsBulkMatchingConfirmRequest
import app.bottlenote.mfds.dto.request.MfdsBulkMatchingPreviewRequest
import app.bottlenote.mfds.dto.request.MfdsMatchingConfirmRequest
import app.bottlenote.mfds.presentation.docs.AdminMfdsMatchingApiDocs
import app.bottlenote.mfds.service.MfdsBulkMatchingService
import app.bottlenote.mfds.service.MfdsMatchingService
import app.bottlenote.user.exception.UserException
import app.bottlenote.user.exception.UserExceptionCode
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/mfds/declarations/{declarationId}/matching")
@AdminMfdsMatchingApiDocs.ApiTag
class AdminMfdsMatchingController(
	private val mfdsMatchingService: MfdsMatchingService,
	private val mfdsBulkMatchingService: MfdsBulkMatchingService
) {
	@AdminMfdsMatchingApiDocs.RunMatching
	@PostMapping("/run")
	fun runMatching(
		@PathVariable declarationId: Long
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(mfdsMatchingService.runMatching(declarationId))

	@AdminMfdsMatchingApiDocs.GetCandidates
	@GetMapping("/candidates")
	fun getCandidates(
		@PathVariable declarationId: Long
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(mfdsMatchingService.getCandidates(declarationId))

	@AdminMfdsMatchingApiDocs.ConfirmMatching
	@PostMapping("/confirm")
	fun confirmMatching(
		@PathVariable declarationId: Long,
		@RequestBody @Valid request: MfdsMatchingConfirmRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(mfdsMatchingService.confirmMatching(declarationId, request, requiredAdminId()))

	@AdminMfdsMatchingApiDocs.ReleaseMatching
	@PostMapping("/release")
	fun releaseMatching(
		@PathVariable declarationId: Long
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(mfdsMatchingService.clearMatching(declarationId, requiredAdminId()))

	@AdminMfdsMatchingApiDocs.PreviewBulkMatching
	@PostMapping("/bulk-preview")
	fun previewBulkMatching(
		@PathVariable declarationId: Long,
		@RequestBody @Valid request: MfdsBulkMatchingPreviewRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(mfdsBulkMatchingService.preview(declarationId, request))

	@AdminMfdsMatchingApiDocs.ConfirmBulkMatching
	@PostMapping("/bulk-confirm")
	fun confirmBulkMatching(
		@PathVariable declarationId: Long,
		@RequestBody @Valid request: MfdsBulkMatchingConfirmRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(mfdsBulkMatchingService.confirm(declarationId, request, requiredAdminId()))

	private fun requiredAdminId(): Long = SecurityContextUtil.getAdminUserIdByContext()
		.orElseThrow { UserException(UserExceptionCode.REQUIRED_USER_ID) }
}
