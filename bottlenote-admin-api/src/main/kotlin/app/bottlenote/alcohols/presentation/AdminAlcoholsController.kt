package app.bottlenote.alcohols.presentation

import app.bottlenote.alcohols.dto.request.AdminAlcoholSearchRequest
import app.bottlenote.alcohols.dto.request.AdminAlcoholUpsertRequest
import app.bottlenote.alcohols.service.AdminAlcoholCommandService
import app.bottlenote.alcohols.service.AlcoholQueryService
import app.bottlenote.global.data.response.GlobalResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/alcohols")
class AdminAlcoholsController(
	private val alcoholQueryService: AlcoholQueryService,
	private val adminAlcoholCommandService: AdminAlcoholCommandService
) {
	@GetMapping
	fun searchAlcohols(
		@ModelAttribute request: AdminAlcoholSearchRequest
	): ResponseEntity<GlobalResponse> = ResponseEntity.ok(alcoholQueryService.searchAdminAlcohols(request))

	@GetMapping("/{alcoholId}")
	fun getAlcoholDetail(
		@PathVariable alcoholId: Long
	): ResponseEntity<*> = GlobalResponse.ok(alcoholQueryService.findAdminAlcoholDetailById(alcoholId))

	@GetMapping("/categories/reference")
	fun getCategoryReference(): ResponseEntity<*> {
		val pairs = alcoholQueryService.findAllCategoryPairs()
		val response = pairs.map { mapOf("korCategory" to it.left, "engCategory" to it.right) }
		return GlobalResponse.ok(response)
	}

	@PostMapping
	fun createAlcohol(
		@RequestBody @Valid request: AdminAlcoholUpsertRequest
	): ResponseEntity<*> = GlobalResponse.ok(adminAlcoholCommandService.createAlcohol(request))

	@PutMapping("/{alcoholId}")
	fun updateAlcohol(
		@PathVariable alcoholId: Long,
		@RequestBody @Valid request: AdminAlcoholUpsertRequest
	): ResponseEntity<*> = GlobalResponse.ok(adminAlcoholCommandService.updateAlcohol(alcoholId, request))

	@DeleteMapping("/{alcoholId}")
	fun deleteAlcohol(
		@PathVariable alcoholId: Long
	): ResponseEntity<*> = GlobalResponse.ok(adminAlcoholCommandService.deleteAlcohol(alcoholId))
}
