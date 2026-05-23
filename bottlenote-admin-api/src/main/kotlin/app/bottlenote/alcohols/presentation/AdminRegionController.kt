package app.bottlenote.alcohols.presentation

import app.bottlenote.alcohols.dto.request.AdminReferenceSearchRequest
import app.bottlenote.alcohols.dto.request.AdminRegionCreateRequest
import app.bottlenote.alcohols.dto.request.AdminRegionSortOrderRequest
import app.bottlenote.alcohols.dto.request.AdminRegionUpdateRequest
import app.bottlenote.alcohols.service.AdminRegionService
import app.bottlenote.alcohols.service.AlcoholReferenceService
import app.bottlenote.global.data.response.GlobalResponse
import app.bottlenote.global.dto.request.AdminBulkReorderRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/regions")
class AdminRegionController(
	private val alcoholReferenceService: AlcoholReferenceService,
	private val adminRegionService: AdminRegionService
) {
	@GetMapping
	fun getAllRegions(
		@ModelAttribute request: AdminReferenceSearchRequest
	): ResponseEntity<*> = ResponseEntity.ok(alcoholReferenceService.findAllRegionsForAdmin(request))

	@GetMapping("/{regionId}")
	fun detail(
		@PathVariable regionId: Long
	): ResponseEntity<*> = GlobalResponse.ok(adminRegionService.getDetail(regionId))

	@PostMapping
	fun create(
		@RequestBody @Valid request: AdminRegionCreateRequest
	): ResponseEntity<*> = GlobalResponse.ok(adminRegionService.create(request))

	@PutMapping("/{regionId}")
	fun update(
		@PathVariable regionId: Long,
		@RequestBody @Valid request: AdminRegionUpdateRequest
	): ResponseEntity<*> = GlobalResponse.ok(adminRegionService.update(regionId, request))

	@DeleteMapping("/{regionId}")
	fun delete(
		@PathVariable regionId: Long
	): ResponseEntity<*> = GlobalResponse.ok(adminRegionService.delete(regionId))

	@PatchMapping("/{regionId}/sort-order")
	fun updateSortOrder(
		@PathVariable regionId: Long,
		@RequestBody @Valid request: AdminRegionSortOrderRequest
	): ResponseEntity<*> = GlobalResponse.ok(adminRegionService.updateSortOrder(regionId, request))

	@PatchMapping("/bulk/reorder")
	fun reorder(
		@RequestBody @Valid request: AdminBulkReorderRequest
	): ResponseEntity<*> = GlobalResponse.ok(adminRegionService.reorder(request))

	@PatchMapping("/{parentId}/children/bulk/reorder")
	fun reorderChildren(
		@PathVariable parentId: Long,
		@RequestBody @Valid request: AdminBulkReorderRequest
	): ResponseEntity<*> = GlobalResponse.ok(adminRegionService.reorderChildren(parentId, request))
}
