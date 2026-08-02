package app.bottlenote.curation.presentation

import app.bottlenote.curation.dto.request.CurationCreateRequest
import app.bottlenote.curation.dto.request.CurationSearchRequest
import app.bottlenote.curation.dto.request.CurationUpdateRequest
import app.bottlenote.curation.presentation.docs.AdminSpecBasedCurationApiDocs
import app.bottlenote.curation.service.AdminSpecBasedCurationService
import app.bottlenote.global.data.response.GlobalResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v2/curations")
@AdminSpecBasedCurationApiDocs.ApiTag
class AdminSpecBasedCurationController(
	private val adminSpecBasedCurationService: AdminSpecBasedCurationService
) {
	@AdminSpecBasedCurationApiDocs.GetAllSpecBasedCurations
	@GetMapping
	fun list(
		@ModelAttribute request: CurationSearchRequest
	): ResponseEntity<GlobalResponse> = ResponseEntity.ok(adminSpecBasedCurationService.search(request))

	@AdminSpecBasedCurationApiDocs.GetSpecBasedCurationFeed
	@GetMapping("/feed")
	fun feed(
		@ModelAttribute request: CurationSearchRequest
	): ResponseEntity<GlobalResponse> = ResponseEntity.ok(adminSpecBasedCurationService.searchFeed(request))

	@AdminSpecBasedCurationApiDocs.GetSpecBasedCurationDetail
	@GetMapping("/{curationId}")
	fun detail(
		@PathVariable curationId: Long
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminSpecBasedCurationService.getDetail(curationId))

	@AdminSpecBasedCurationApiDocs.CreateSpecBasedCuration
	@PostMapping
	fun create(
		@RequestBody @Valid request: CurationCreateRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminSpecBasedCurationService.create(request))

	@AdminSpecBasedCurationApiDocs.UpdateSpecBasedCuration
	@PutMapping("/{curationId}")
	fun update(
		@PathVariable curationId: Long,
		@RequestBody @Valid request: CurationUpdateRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminSpecBasedCurationService.update(curationId, request))
}
