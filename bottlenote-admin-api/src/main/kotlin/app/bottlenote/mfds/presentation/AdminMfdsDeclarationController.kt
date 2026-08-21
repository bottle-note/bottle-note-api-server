package app.bottlenote.mfds.presentation

import app.bottlenote.global.data.response.GlobalResponse
import app.bottlenote.mfds.dto.request.MfdsDeclarationImporterLinkRequest
import app.bottlenote.mfds.dto.request.MfdsDeclarationSearchRequest
import app.bottlenote.mfds.dto.request.MfdsDeclarationStatusRequest
import app.bottlenote.mfds.presentation.docs.AdminMfdsDeclarationApiDocs
import app.bottlenote.mfds.service.MfdsDeclarationService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/mfds/declarations")
@AdminMfdsDeclarationApiDocs.ApiTag
class AdminMfdsDeclarationController(
	private val mfdsDeclarationService: MfdsDeclarationService
) {
	@AdminMfdsDeclarationApiDocs.SearchDeclarations
	@GetMapping
	fun searchDeclarations(
		@ModelAttribute request: MfdsDeclarationSearchRequest
	): ResponseEntity<GlobalResponse> = ResponseEntity.ok(mfdsDeclarationService.search(request))

	@AdminMfdsDeclarationApiDocs.GetDeclarationDetail
	@GetMapping("/{declarationId}")
	fun getDeclarationDetail(
		@PathVariable declarationId: Long
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(mfdsDeclarationService.getDetail(declarationId))

	@AdminMfdsDeclarationApiDocs.ChangeNormalizationStatus
	@PatchMapping("/{declarationId}/normalization-status")
	fun changeNormalizationStatus(
		@PathVariable declarationId: Long,
		@RequestBody @Valid request: MfdsDeclarationStatusRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(mfdsDeclarationService.changeNormalizationStatus(declarationId, request))

	@AdminMfdsDeclarationApiDocs.LinkImporter
	@PostMapping("/{declarationId}/importer")
	fun linkImporter(
		@PathVariable declarationId: Long,
		@RequestBody @Valid request: MfdsDeclarationImporterLinkRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(mfdsDeclarationService.linkImporter(declarationId, request))

	@AdminMfdsDeclarationApiDocs.UnlinkImporter
	@DeleteMapping("/{declarationId}/importer")
	fun unlinkImporter(
		@PathVariable declarationId: Long
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(mfdsDeclarationService.unlinkImporter(declarationId))
}
