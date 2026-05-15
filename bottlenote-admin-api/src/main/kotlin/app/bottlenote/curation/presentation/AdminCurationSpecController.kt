package app.bottlenote.curation.presentation

import app.bottlenote.curation.service.AdminSpecBasedCurationService
import app.bottlenote.global.data.response.GlobalResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/curation-specs")
class AdminCurationSpecController(
	private val adminSpecBasedCurationService: AdminSpecBasedCurationService
) {
	@GetMapping
	fun list(): ResponseEntity<*> = GlobalResponse.ok(adminSpecBasedCurationService.listSpecs())

	@GetMapping("/{specId}")
	fun detail(
		@PathVariable specId: Long
	): ResponseEntity<*> = GlobalResponse.ok(adminSpecBasedCurationService.getSpecDetail(specId))
}
