package app.bottlenote.mfds.presentation

import app.bottlenote.global.data.response.GlobalResponse
import app.bottlenote.global.security.SecurityContextUtil
import app.bottlenote.mfds.dto.request.MfdsBulkMatchingConfirmRequest
import app.bottlenote.mfds.dto.request.MfdsBulkMatchingPreviewRequest
import app.bottlenote.mfds.presentation.docs.AdminMfdsMatchingApiDocs
import app.bottlenote.mfds.service.MfdsBulkMatchingService
import app.bottlenote.user.exception.UserException
import app.bottlenote.user.exception.UserExceptionCode
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/mfds/matching")
@AdminMfdsMatchingApiDocs.ApiTag
class AdminMfdsBulkMatchingController(
	private val mfdsBulkMatchingService: MfdsBulkMatchingService
) {
	@AdminMfdsMatchingApiDocs.PreviewBulkMatching
	@PostMapping("/preview")
	fun preview(
		@RequestBody @Valid request: MfdsBulkMatchingPreviewRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(mfdsBulkMatchingService.preview(request, requiredAdminId()))

	@AdminMfdsMatchingApiDocs.ConfirmBulkMatching
	@PostMapping("/confirm")
	fun confirm(
		@RequestBody @Valid request: MfdsBulkMatchingConfirmRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(mfdsBulkMatchingService.confirm(request, requiredAdminId()))

	private fun requiredAdminId(): Long = SecurityContextUtil.getAdminUserIdByContext()
		.orElseThrow { UserException(UserExceptionCode.REQUIRED_USER_ID) }
}
