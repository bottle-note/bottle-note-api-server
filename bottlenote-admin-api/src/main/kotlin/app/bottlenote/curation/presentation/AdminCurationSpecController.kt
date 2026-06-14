package app.bottlenote.curation.presentation

import app.bottlenote.curation.service.CurationSpecQueryService
import app.bottlenote.global.data.response.GlobalResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v2/curation-specs")
class AdminCurationSpecController(
	private val curationSpecQueryService: CurationSpecQueryService
) {
	@GetMapping
	fun list(): ResponseEntity<*> = GlobalResponse.ok(curationSpecQueryService.listActiveSpecs())

	@GetMapping("/{specId}")
	fun detail(
		@PathVariable specId: Long
	): ResponseEntity<*> = GlobalResponse.ok(curationSpecQueryService.getSpecDetail(specId))
}
