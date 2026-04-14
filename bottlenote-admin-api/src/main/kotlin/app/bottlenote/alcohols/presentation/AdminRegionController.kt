package app.bottlenote.alcohols.presentation

import app.bottlenote.alcohols.dto.request.AdminReferenceSearchRequest
import app.bottlenote.alcohols.service.AlcoholReferenceService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/regions")
class AdminRegionController(
	private val alcoholReferenceService: AlcoholReferenceService
) {
	@GetMapping
	fun getAllRegions(
		@ModelAttribute request: AdminReferenceSearchRequest
	): ResponseEntity<*> = ResponseEntity.ok(alcoholReferenceService.findAllRegionsForAdmin(request))
}
