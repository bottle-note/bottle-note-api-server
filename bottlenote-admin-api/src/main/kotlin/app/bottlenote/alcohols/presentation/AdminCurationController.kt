package app.bottlenote.alcohols.presentation

import app.bottlenote.alcohols.dto.request.*
import app.bottlenote.alcohols.service.AdminCurationService
import app.bottlenote.global.data.response.GlobalResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/curations")
class AdminCurationController(
	private val adminCurationService: AdminCurationService
) {
	@GetMapping
	fun list(
		@ModelAttribute request: AdminCurationSearchRequest
	): ResponseEntity<GlobalResponse> = ResponseEntity.ok(adminCurationService.search(request))

	@GetMapping("/{curationId}")
	fun detail(
		@PathVariable curationId: Long
	): ResponseEntity<*> = GlobalResponse.ok(adminCurationService.getDetail(curationId))

	@PostMapping
	fun create(
		@RequestBody @Valid request: AdminCurationCreateRequest
	): ResponseEntity<*> = GlobalResponse.ok(adminCurationService.create(request))

	@PutMapping("/{curationId}")
	fun update(
		@PathVariable curationId: Long,
		@RequestBody @Valid request: AdminCurationUpdateRequest
	): ResponseEntity<*> = GlobalResponse.ok(adminCurationService.update(curationId, request))

	@DeleteMapping("/{curationId}")
	fun delete(
		@PathVariable curationId: Long
	): ResponseEntity<*> = GlobalResponse.ok(adminCurationService.delete(curationId))

	@PatchMapping("/{curationId}/status")
	fun updateStatus(
		@PathVariable curationId: Long,
		@RequestBody @Valid request: AdminCurationStatusRequest
	): ResponseEntity<*> = GlobalResponse.ok(adminCurationService.updateStatus(curationId, request))

	@PatchMapping("/{curationId}/display-order")
	fun updateDisplayOrder(
		@PathVariable curationId: Long,
		@RequestBody @Valid request: AdminCurationDisplayOrderRequest
	): ResponseEntity<*> = GlobalResponse.ok(adminCurationService.updateDisplayOrder(curationId, request))

	@PostMapping("/{curationId}/alcohols")
	fun addAlcohols(
		@PathVariable curationId: Long,
		@RequestBody @Valid request: AdminCurationAlcoholRequest
	): ResponseEntity<*> = GlobalResponse.ok(adminCurationService.addAlcohols(curationId, request))

	@DeleteMapping("/{curationId}/alcohols/{alcoholId}")
	fun removeAlcohol(
		@PathVariable curationId: Long,
		@PathVariable alcoholId: Long
	): ResponseEntity<*> = GlobalResponse.ok(adminCurationService.removeAlcohol(curationId, alcoholId))
}
