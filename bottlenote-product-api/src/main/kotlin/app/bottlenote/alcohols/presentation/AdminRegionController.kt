package app.bottlenote.alcohols.presentation

import app.bottlenote.alcohols.dto.request.AdminReferenceSearchRequest
import app.bottlenote.alcohols.dto.request.AdminRegionCreateRequest
import app.bottlenote.alcohols.dto.request.AdminRegionSortOrderRequest
import app.bottlenote.alcohols.dto.request.AdminRegionUpdateRequest
import app.bottlenote.alcohols.presentation.docs.AdminRegionApiDocs
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
@AdminRegionApiDocs.ApiTag
class AdminRegionController(
	private val alcoholReferenceService: AlcoholReferenceService,
	private val adminRegionService: AdminRegionService
) {
	@AdminRegionApiDocs.GetAllRegions
	@GetMapping
	fun getAllRegions(
		@ModelAttribute request: AdminReferenceSearchRequest
	): ResponseEntity<GlobalResponse> = ResponseEntity.ok(alcoholReferenceService.findAllRegionsForAdmin(request))

	@AdminRegionApiDocs.GetRegionDetail
	@GetMapping("/{regionId}")
	fun detail(
		@PathVariable regionId: Long
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminRegionService.getDetail(regionId))

	@AdminRegionApiDocs.CreateRegion
	@PostMapping
	fun create(
		@RequestBody @Valid request: AdminRegionCreateRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminRegionService.create(request))

	@AdminRegionApiDocs.UpdateRegion
	@PutMapping("/{regionId}")
	fun update(
		@PathVariable regionId: Long,
		@RequestBody @Valid request: AdminRegionUpdateRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminRegionService.update(regionId, request))

	@AdminRegionApiDocs.DeleteRegion
	@DeleteMapping("/{regionId}")
	fun delete(
		@PathVariable regionId: Long
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminRegionService.delete(regionId))

	@AdminRegionApiDocs.UpdateRegionSortOrder
	@PatchMapping("/{regionId}/sort-order")
	fun updateSortOrder(
		@PathVariable regionId: Long,
		@RequestBody @Valid request: AdminRegionSortOrderRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminRegionService.updateSortOrder(regionId, request))

	@AdminRegionApiDocs.ReorderRegions
	@PatchMapping("/bulk/reorder")
	fun reorder(
		@RequestBody @Valid request: AdminBulkReorderRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminRegionService.reorder(request))

	@AdminRegionApiDocs.ReorderRegionChildren
	@PatchMapping("/{parentId}/children/bulk/reorder")
	fun reorderChildren(
		@PathVariable parentId: Long,
		@RequestBody @Valid request: AdminBulkReorderRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminRegionService.reorderChildren(parentId, request))
}
