package app.bottlenote.alcohols.presentation

import app.bottlenote.alcohols.dto.request.AdminAlcoholSearchRequest
import app.bottlenote.alcohols.service.AlcoholQueryService
import app.bottlenote.global.data.response.GlobalResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/alcohols")
class AdminAlcoholsController(
	private val alcoholQueryService: AlcoholQueryService
) {

	@GetMapping
	fun searchAlcohols(@ModelAttribute request: AdminAlcoholSearchRequest): ResponseEntity<GlobalResponse> {
		return ResponseEntity.ok(alcoholQueryService.searchAdminAlcohols(request))
	}

	@GetMapping("/{alcoholId}")
	fun getAlcoholDetail(@PathVariable alcoholId: Long): ResponseEntity<*> {
		return GlobalResponse.ok(alcoholQueryService.findAdminAlcoholDetailById(alcoholId))
	}
}
