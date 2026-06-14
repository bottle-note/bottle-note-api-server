package app.bottlenote.curation.presentation

import app.bottlenote.curation.dto.request.CurationCreateRequest
import app.bottlenote.curation.dto.request.CurationSearchRequest
import app.bottlenote.curation.dto.request.CurationUpdateRequest
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
class AdminSpecBasedCurationController(
	private val adminSpecBasedCurationService: AdminSpecBasedCurationService
) {
	@GetMapping
	fun list(
		@ModelAttribute request: CurationSearchRequest
	): ResponseEntity<GlobalResponse> = ResponseEntity.ok(adminSpecBasedCurationService.search(request))

	@GetMapping("/feed")
	fun feed(
		@ModelAttribute request: CurationSearchRequest
	): ResponseEntity<GlobalResponse> = ResponseEntity.ok(adminSpecBasedCurationService.searchFeed(request))

	@GetMapping("/{curationId}")
	fun detail(
		@PathVariable curationId: Long
	): ResponseEntity<*> = GlobalResponse.ok(adminSpecBasedCurationService.getDetail(curationId))

	@PostMapping
	fun create(
		@RequestBody @Valid request: CurationCreateRequest
	): ResponseEntity<*> = GlobalResponse.ok(adminSpecBasedCurationService.create(request))

	@PutMapping("/{curationId}")
	fun update(
		@PathVariable curationId: Long,
		@RequestBody @Valid request: CurationUpdateRequest
	): ResponseEntity<*> = GlobalResponse.ok(adminSpecBasedCurationService.update(curationId, request))
}
