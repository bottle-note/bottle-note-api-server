package app.bottlenote.curation.presentation

import app.bottlenote.curation.presentation.docs.AdminCurationSpecApiDocs
import app.bottlenote.curation.service.CurationSpecQueryService
import app.bottlenote.global.data.response.GlobalResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v2/curation-specs")
@AdminCurationSpecApiDocs.ApiTag
class AdminCurationSpecController(
	private val curationSpecQueryService: CurationSpecQueryService
) {
	@AdminCurationSpecApiDocs.GetAllCurationSpecs
	@GetMapping
	fun list(): ResponseEntity<GlobalResponse> = GlobalResponse.ok(curationSpecQueryService.listActiveSpecs())

	@AdminCurationSpecApiDocs.GetCurationSpecDetail
	@GetMapping("/{specId}")
	fun detail(
		@PathVariable specId: Long
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(curationSpecQueryService.getSpecDetail(specId))
}
