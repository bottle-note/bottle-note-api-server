package app.bottlenote.alcohols.presentation

import app.bottlenote.alcohols.dto.request.AdminCurationAlcoholRequest
import app.bottlenote.alcohols.dto.request.AdminCurationCreateRequest
import app.bottlenote.alcohols.dto.request.AdminCurationDisplayOrderRequest
import app.bottlenote.alcohols.dto.request.AdminCurationSearchRequest
import app.bottlenote.alcohols.dto.request.AdminCurationStatusRequest
import app.bottlenote.alcohols.dto.request.AdminCurationUpdateRequest
import app.bottlenote.alcohols.service.AdminCurationService
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
@RequestMapping("/curations")
class AdminCurationController(
    private val adminCurationService: AdminCurationService
) {

    @GetMapping
    fun list(@ModelAttribute request: AdminCurationSearchRequest): ResponseEntity<GlobalResponse> {
        return ResponseEntity.ok(adminCurationService.search(request))
    }

    @GetMapping("/{curationId}")
    fun detail(@PathVariable curationId: Long): ResponseEntity<*> {
        return GlobalResponse.ok(adminCurationService.getDetail(curationId))
    }

    @PostMapping
    fun create(@RequestBody @Valid request: AdminCurationCreateRequest): ResponseEntity<*> {
        return GlobalResponse.ok(adminCurationService.create(request))
    }

    @PutMapping("/{curationId}")
    fun update(
        @PathVariable curationId: Long,
        @RequestBody @Valid request: AdminCurationUpdateRequest
    ): ResponseEntity<*> {
        return GlobalResponse.ok(adminCurationService.update(curationId, request))
    }

    @DeleteMapping("/{curationId}")
    fun delete(@PathVariable curationId: Long): ResponseEntity<*> {
        return GlobalResponse.ok(adminCurationService.delete(curationId))
    }

    @PatchMapping("/{curationId}/status")
    fun updateStatus(
        @PathVariable curationId: Long,
        @RequestBody @Valid request: AdminCurationStatusRequest
    ): ResponseEntity<*> {
        return GlobalResponse.ok(adminCurationService.updateStatus(curationId, request))
    }

    @PatchMapping("/{curationId}/display-order")
    fun updateDisplayOrder(
        @PathVariable curationId: Long,
        @RequestBody @Valid request: AdminCurationDisplayOrderRequest
    ): ResponseEntity<*> {
        return GlobalResponse.ok(adminCurationService.updateDisplayOrder(curationId, request))
    }

    @PostMapping("/{curationId}/alcohols")
    fun addAlcohols(
        @PathVariable curationId: Long,
        @RequestBody @Valid request: AdminCurationAlcoholRequest
    ): ResponseEntity<*> {
        return GlobalResponse.ok(adminCurationService.addAlcohols(curationId, request))
    }

    @DeleteMapping("/{curationId}/alcohols/{alcoholId}")
    fun removeAlcohol(
        @PathVariable curationId: Long,
        @PathVariable alcoholId: Long
    ): ResponseEntity<*> {
        return GlobalResponse.ok(adminCurationService.removeAlcohol(curationId, alcoholId))
    }
}
