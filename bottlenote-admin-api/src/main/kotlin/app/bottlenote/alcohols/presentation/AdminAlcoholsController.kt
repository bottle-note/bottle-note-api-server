package app.bottlenote.alcohols.presentation

import app.bottlenote.alcohols.dto.request.AdminAlcoholSearchRequest
import app.bottlenote.alcohols.dto.request.AdminAlcoholUpsertRequest
import app.bottlenote.alcohols.service.AdminAlcoholCommandService
import app.bottlenote.alcohols.service.AlcoholQueryService
import app.bottlenote.global.data.response.GlobalResponse
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
@RequestMapping("/alcohols")
class AdminAlcoholsController(
	private val alcoholQueryService: AlcoholQueryService,
	private val adminAlcoholCommandService: AdminAlcoholCommandService
) {

	@GetMapping
	fun searchAlcohols(@ModelAttribute request: AdminAlcoholSearchRequest): ResponseEntity<GlobalResponse> {
		return ResponseEntity.ok(alcoholQueryService.searchAdminAlcohols(request))
	}

	@GetMapping("/{alcoholId}")
	fun getAlcoholDetail(@PathVariable alcoholId: Long): ResponseEntity<*> {
		return GlobalResponse.ok(alcoholQueryService.findAdminAlcoholDetailById(alcoholId))
	}

	@GetMapping("/categories/reference")
	fun getCategoryReference(): ResponseEntity<*> {
		val pairs = alcoholQueryService.findAllCategoryPairs()
		val response = pairs.map { mapOf("korCategory" to it.left, "engCategory" to it.right) }
		return GlobalResponse.ok(response)
	}

	@PostMapping
	fun createAlcohol(@RequestBody @Valid request: AdminAlcoholUpsertRequest): ResponseEntity<*> {
		return GlobalResponse.ok(adminAlcoholCommandService.createAlcohol(request))
	}

	@PutMapping("/{alcoholId}")
	fun updateAlcohol(
		@PathVariable alcoholId: Long,
		@RequestBody @Valid request: AdminAlcoholUpsertRequest
	): ResponseEntity<*> {
		return GlobalResponse.ok(adminAlcoholCommandService.updateAlcohol(alcoholId, request))
	}

	@DeleteMapping("/{alcoholId}")
	fun deleteAlcohol(@PathVariable alcoholId: Long): ResponseEntity<*> {
		return GlobalResponse.ok(adminAlcoholCommandService.deleteAlcohol(alcoholId))
	}
}
