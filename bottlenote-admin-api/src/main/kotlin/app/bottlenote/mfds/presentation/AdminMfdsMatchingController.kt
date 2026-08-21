package app.bottlenote.mfds.presentation

import app.bottlenote.global.data.response.GlobalResponse
import app.bottlenote.mfds.dto.request.MfdsMatchingConfirmRequest
import app.bottlenote.mfds.presentation.docs.AdminMfdsMatchingApiDocs
import app.bottlenote.mfds.service.MfdsMatchingService
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
	private val mfdsMatchingService: MfdsMatchingService
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
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(mfdsMatchingService.confirmMatching(declarationId, request))

	@AdminMfdsMatchingApiDocs.ReleaseMatching
	@PostMapping("/release")
	fun releaseMatching(
		@PathVariable declarationId: Long
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(mfdsMatchingService.clearMatching(declarationId))
}
