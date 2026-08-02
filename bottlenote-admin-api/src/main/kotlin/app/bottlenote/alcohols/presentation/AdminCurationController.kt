package app.bottlenote.alcohols.presentation

import app.bottlenote.alcohols.dto.request.*
import app.bottlenote.alcohols.presentation.docs.AdminCurationApiDocs
import app.bottlenote.alcohols.service.AdminCurationService
import app.bottlenote.global.data.response.GlobalResponse
import app.bottlenote.global.dto.request.AdminBulkReorderRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/curations")
@AdminCurationApiDocs.ApiTag
class AdminCurationController(
	private val adminCurationService: AdminCurationService
) {
	@AdminCurationApiDocs.GetAllCurations
	@GetMapping
	fun list(
		@ModelAttribute request: AdminCurationSearchRequest
	): ResponseEntity<GlobalResponse> = ResponseEntity.ok(adminCurationService.search(request))

	@AdminCurationApiDocs.GetCurationDetail
	@GetMapping("/{curationId}")
	fun detail(
		@PathVariable curationId: Long
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminCurationService.getDetail(curationId))

	@AdminCurationApiDocs.CreateCuration
	@PostMapping
	fun create(
		@RequestBody @Valid request: AdminCurationCreateRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminCurationService.create(request))

	@AdminCurationApiDocs.UpdateCuration
	@PutMapping("/{curationId}")
	fun update(
		@PathVariable curationId: Long,
		@RequestBody @Valid request: AdminCurationUpdateRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminCurationService.update(curationId, request))

	@AdminCurationApiDocs.DeleteCuration
	@DeleteMapping("/{curationId}")
	fun delete(
		@PathVariable curationId: Long
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminCurationService.delete(curationId))

	@AdminCurationApiDocs.UpdateCurationStatus
	@PatchMapping("/{curationId}/status")
	fun updateStatus(
		@PathVariable curationId: Long,
		@RequestBody @Valid request: AdminCurationStatusRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminCurationService.updateStatus(curationId, request))

	@AdminCurationApiDocs.UpdateCurationDisplayOrder
	@PatchMapping("/{curationId}/display-order")
	fun updateDisplayOrder(
		@PathVariable curationId: Long,
		@RequestBody @Valid request: AdminCurationDisplayOrderRequest
	): ResponseEntity<GlobalResponse> =
		GlobalResponse.ok(adminCurationService.updateDisplayOrder(curationId, request))

	@AdminCurationApiDocs.ReorderCurations
	@PatchMapping("/bulk/reorder")
	fun reorder(
		@RequestBody @Valid request: AdminBulkReorderRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminCurationService.reorder(request))

	@AdminCurationApiDocs.AddAlcoholsToCuration
	@PostMapping("/{curationId}/alcohols")
	fun addAlcohols(
		@PathVariable curationId: Long,
		@RequestBody @Valid request: AdminCurationAlcoholRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminCurationService.addAlcohols(curationId, request))

	@AdminCurationApiDocs.RemoveAlcoholFromCuration
	@DeleteMapping("/{curationId}/alcohols/{alcoholId}")
	fun removeAlcohol(
		@PathVariable curationId: Long,
		@PathVariable alcoholId: Long
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminCurationService.removeAlcohol(curationId, alcoholId))
}
