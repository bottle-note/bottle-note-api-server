package app.bottlenote.alcohols.persentaton

import app.bottlenote.alcohols.domain.RegionRepository
import app.bottlenote.alcohols.dto.request.AdminReferenceSearchRequest
import app.bottlenote.global.data.response.GlobalResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/regions")
class AdminRegionController(
	private val regionRepository: RegionRepository
) {

	@GetMapping
	fun getAllRegions(@ModelAttribute request: AdminReferenceSearchRequest): ResponseEntity<*> {
		val page = regionRepository.findAllRegions(request.keyword(), request.toPageable())
		return ResponseEntity.ok(GlobalResponse.fromPage(page))
	}
}
