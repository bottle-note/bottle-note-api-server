package app.bottlenote.banner.presentation

import app.bottlenote.banner.dto.request.AdminBannerCreateRequest
import app.bottlenote.banner.dto.request.AdminBannerSearchRequest
import app.bottlenote.banner.dto.request.AdminBannerSortOrderRequest
import app.bottlenote.banner.dto.request.AdminBannerStatusRequest
import app.bottlenote.banner.dto.request.AdminBannerUpdateRequest
import app.bottlenote.banner.service.AdminBannerService
import app.bottlenote.global.data.response.GlobalResponse
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
@RequestMapping("/banners")
class AdminBannerController(
    private val adminBannerService: AdminBannerService
) {

    @GetMapping
    fun list(@ModelAttribute request: AdminBannerSearchRequest): ResponseEntity<GlobalResponse> {
        return ResponseEntity.ok(adminBannerService.search(request))
    }

    @GetMapping("/{bannerId}")
    fun detail(@PathVariable bannerId: Long): ResponseEntity<*> {
        return GlobalResponse.ok(adminBannerService.getDetail(bannerId))
    }

    @PostMapping
    fun create(@RequestBody @Valid request: AdminBannerCreateRequest): ResponseEntity<*> {
        return GlobalResponse.ok(adminBannerService.create(request))
    }

    @PutMapping("/{bannerId}")
    fun update(
        @PathVariable bannerId: Long,
        @RequestBody @Valid request: AdminBannerUpdateRequest
    ): ResponseEntity<*> {
        return GlobalResponse.ok(adminBannerService.update(bannerId, request))
    }

    @DeleteMapping("/{bannerId}")
    fun delete(@PathVariable bannerId: Long): ResponseEntity<*> {
        return GlobalResponse.ok(adminBannerService.delete(bannerId))
    }

    @PatchMapping("/{bannerId}/status")
    fun updateStatus(
        @PathVariable bannerId: Long,
        @RequestBody @Valid request: AdminBannerStatusRequest
    ): ResponseEntity<*> {
        return GlobalResponse.ok(adminBannerService.updateStatus(bannerId, request))
    }

    @PatchMapping("/{bannerId}/sort-order")
    fun updateSortOrder(
        @PathVariable bannerId: Long,
        @RequestBody @Valid request: AdminBannerSortOrderRequest
    ): ResponseEntity<*> {
        return GlobalResponse.ok(adminBannerService.updateSortOrder(bannerId, request))
    }
}
