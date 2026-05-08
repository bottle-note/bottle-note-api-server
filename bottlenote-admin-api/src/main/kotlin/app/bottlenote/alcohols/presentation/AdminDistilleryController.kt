package app.bottlenote.alcohols.presentation

import app.bottlenote.alcohols.dto.request.AdminDistillerySortOrderRequest
import app.bottlenote.alcohols.dto.request.AdminDistilleryUpsertRequest
import app.bottlenote.alcohols.dto.request.AdminReferenceSearchRequest
import app.bottlenote.alcohols.service.AlcoholReferenceService
import app.bottlenote.alcohols.service.DistilleryService
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
@RequestMapping("/distilleries")
class AdminDistilleryController(
	private val alcoholReferenceService: AlcoholReferenceService,
	private val distilleryService: DistilleryService
) {
	@GetMapping
	fun getAllDistilleries(
		@ModelAttribute request: AdminReferenceSearchRequest
	): ResponseEntity<*> = ResponseEntity.ok(alcoholReferenceService.findAllDistilleries(request))

	@GetMapping("/{distilleryId}")
	fun getDistilleryDetail(
		@PathVariable distilleryId: Long
	): ResponseEntity<*> = GlobalResponse.ok(distilleryService.getDetail(distilleryId))

	@PostMapping
	fun createDistillery(
		@RequestBody @Valid request: AdminDistilleryUpsertRequest
	): ResponseEntity<*> = GlobalResponse.ok(distilleryService.create(request))

	@PutMapping("/{distilleryId}")
	fun updateDistillery(
		@PathVariable distilleryId: Long,
		@RequestBody @Valid request: AdminDistilleryUpsertRequest
	): ResponseEntity<*> = GlobalResponse.ok(distilleryService.update(distilleryId, request))

	@DeleteMapping("/{distilleryId}")
	fun deleteDistillery(
		@PathVariable distilleryId: Long
	): ResponseEntity<*> = GlobalResponse.ok(distilleryService.delete(distilleryId))

	@PatchMapping("/{distilleryId}/sort-order")
	fun updateSortOrder(
		@PathVariable distilleryId: Long,
		@RequestBody @Valid request: AdminDistillerySortOrderRequest
	): ResponseEntity<*> = GlobalResponse.ok(distilleryService.updateSortOrder(distilleryId, request))
}
