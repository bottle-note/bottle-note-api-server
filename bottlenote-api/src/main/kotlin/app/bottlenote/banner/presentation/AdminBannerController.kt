package app.bottlenote.banner.presentation

import app.bottlenote.banner.dto.request.*
import app.bottlenote.banner.presentation.docs.AdminBannerApiDocs
import app.bottlenote.banner.service.AdminBannerService
import app.bottlenote.global.data.response.GlobalResponse
import app.bottlenote.global.dto.request.AdminBulkReorderRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/banners")
@AdminBannerApiDocs.ApiTag
class AdminBannerController(
	private val adminBannerService: AdminBannerService
) {
	@AdminBannerApiDocs.GetAllBanners
	@GetMapping
	fun list(
		@ModelAttribute request: AdminBannerSearchRequest
	): ResponseEntity<GlobalResponse> = ResponseEntity.ok(adminBannerService.search(request))

	@AdminBannerApiDocs.GetBannerDetail
	@GetMapping("/{bannerId}")
	fun detail(
		@PathVariable bannerId: Long
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminBannerService.getDetail(bannerId))

	@AdminBannerApiDocs.CreateBanner
	@PostMapping
	fun create(
		@RequestBody @Valid request: AdminBannerCreateRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminBannerService.create(request))

	@AdminBannerApiDocs.UpdateBanner
	@PutMapping("/{bannerId}")
	fun update(
		@PathVariable bannerId: Long,
		@RequestBody @Valid request: AdminBannerUpdateRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminBannerService.update(bannerId, request))

	@AdminBannerApiDocs.DeleteBanner
	@DeleteMapping("/{bannerId}")
	fun delete(
		@PathVariable bannerId: Long
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminBannerService.delete(bannerId))

	@AdminBannerApiDocs.UpdateBannerStatus
	@PatchMapping("/{bannerId}/status")
	fun updateStatus(
		@PathVariable bannerId: Long,
		@RequestBody @Valid request: AdminBannerStatusRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminBannerService.updateStatus(bannerId, request))

	@AdminBannerApiDocs.UpdateBannerSortOrder
	@PatchMapping("/{bannerId}/sort-order")
	fun updateSortOrder(
		@PathVariable bannerId: Long,
		@RequestBody @Valid request: AdminBannerSortOrderRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminBannerService.updateSortOrder(bannerId, request))

	@AdminBannerApiDocs.ReorderBanners
	@PatchMapping("/bulk/reorder")
	fun reorder(
		@RequestBody @Valid request: AdminBulkReorderRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminBannerService.reorder(request))
}
