package app.bottlenote.mfds.presentation

import app.bottlenote.global.data.response.GlobalResponse
import app.bottlenote.mfds.dto.request.MfdsImporterCreateRequest
import app.bottlenote.mfds.dto.request.MfdsImporterSearchRequest
import app.bottlenote.mfds.dto.request.MfdsImporterUpdateRequest
import app.bottlenote.mfds.presentation.docs.AdminMfdsImporterApiDocs
import app.bottlenote.mfds.service.MfdsImporterService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/mfds/importers")
@AdminMfdsImporterApiDocs.ApiTag
class AdminMfdsImporterController(
	private val mfdsImporterService: MfdsImporterService
) {
	@AdminMfdsImporterApiDocs.SearchImporters
	@GetMapping
	fun searchImporters(
		@ModelAttribute request: MfdsImporterSearchRequest
	): ResponseEntity<GlobalResponse> = ResponseEntity.ok(mfdsImporterService.search(request))

	@AdminMfdsImporterApiDocs.GetImporterDetail
	@GetMapping("/{importerId}")
	fun getImporterDetail(
		@PathVariable importerId: Long
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(mfdsImporterService.getDetail(importerId))

	@AdminMfdsImporterApiDocs.CreateImporter
	@PostMapping
	fun createImporter(
		@RequestBody @Valid request: MfdsImporterCreateRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(mfdsImporterService.create(request))

	@AdminMfdsImporterApiDocs.UpdateImporter
	@PutMapping("/{importerId}")
	fun updateImporter(
		@PathVariable importerId: Long,
		@RequestBody @Valid request: MfdsImporterUpdateRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(mfdsImporterService.update(importerId, request))

	@AdminMfdsImporterApiDocs.DeleteImporter
	@DeleteMapping("/{importerId}")
	fun deleteImporter(
		@PathVariable importerId: Long
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(mfdsImporterService.delete(importerId))
}
