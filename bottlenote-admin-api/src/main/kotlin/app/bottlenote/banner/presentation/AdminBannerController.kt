package app.bottlenote.banner.presentation

import app.bottlenote.banner.dto.request.*
import app.bottlenote.banner.service.AdminBannerService
import app.bottlenote.global.data.response.GlobalResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/banners")
class AdminBannerController(
	private val adminBannerService: AdminBannerService
) {
	@GetMapping
	fun list(
		@ModelAttribute request: AdminBannerSearchRequest
	): ResponseEntity<GlobalResponse> = ResponseEntity.ok(adminBannerService.search(request))

	@GetMapping("/{bannerId}")
	fun detail(
		@PathVariable bannerId: Long
	): ResponseEntity<*> = GlobalResponse.ok(adminBannerService.getDetail(bannerId))

	@PostMapping
	fun create(
		@RequestBody @Valid request: AdminBannerCreateRequest
	): ResponseEntity<*> = GlobalResponse.ok(adminBannerService.create(request))

	@PutMapping("/{bannerId}")
	fun update(
		@PathVariable bannerId: Long,
		@RequestBody @Valid request: AdminBannerUpdateRequest
	): ResponseEntity<*> = GlobalResponse.ok(adminBannerService.update(bannerId, request))

	@DeleteMapping("/{bannerId}")
	fun delete(
		@PathVariable bannerId: Long
	): ResponseEntity<*> = GlobalResponse.ok(adminBannerService.delete(bannerId))

	@PatchMapping("/{bannerId}/status")
	fun updateStatus(
		@PathVariable bannerId: Long,
		@RequestBody @Valid request: AdminBannerStatusRequest
	): ResponseEntity<*> = GlobalResponse.ok(adminBannerService.updateStatus(bannerId, request))

	@PatchMapping("/{bannerId}/sort-order")
	fun updateSortOrder(
		@PathVariable bannerId: Long,
		@RequestBody @Valid request: AdminBannerSortOrderRequest
	): ResponseEntity<*> = GlobalResponse.ok(adminBannerService.updateSortOrder(bannerId, request))
}
