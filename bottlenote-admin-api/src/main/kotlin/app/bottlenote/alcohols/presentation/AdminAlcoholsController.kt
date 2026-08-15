package app.bottlenote.alcohols.presentation

import app.bottlenote.alcohols.dto.request.AdminAlcoholLookupRequest
import app.bottlenote.alcohols.dto.request.AdminAlcoholSearchRequest
import app.bottlenote.alcohols.dto.request.AdminAlcoholUpsertRequest
import app.bottlenote.alcohols.presentation.docs.AdminAlcoholsApiDocs
import app.bottlenote.alcohols.service.AdminAlcoholCommandService
import app.bottlenote.alcohols.service.AdminAlcoholLookupService
import app.bottlenote.alcohols.service.AdminAlcoholQueryService
import app.bottlenote.global.data.response.GlobalResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/alcohols")
@AdminAlcoholsApiDocs.ApiTag
class AdminAlcoholsController(
	private val adminAlcoholQueryService: AdminAlcoholQueryService,
	private val adminAlcoholCommandService: AdminAlcoholCommandService,
	private val adminAlcoholLookupService: AdminAlcoholLookupService
) {
	@AdminAlcoholsApiDocs.GetAlcoholLookups
	@GetMapping("/lookup")
	fun getAlcoholLookups(
		@ModelAttribute @Valid request: AdminAlcoholLookupRequest
	): ResponseEntity<GlobalResponse> = ResponseEntity.ok(adminAlcoholLookupService.lookup(request))

	@AdminAlcoholsApiDocs.SearchAlcohols
	@GetMapping
	fun searchAlcohols(
		@ModelAttribute request: AdminAlcoholSearchRequest
	): ResponseEntity<GlobalResponse> = ResponseEntity.ok(adminAlcoholQueryService.searchAdminAlcohols(request))

	@AdminAlcoholsApiDocs.GetAlcoholDetail
	@GetMapping("/{alcoholId}")
	fun getAlcoholDetail(
		@PathVariable alcoholId: Long
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminAlcoholQueryService.findAdminAlcoholDetailById(alcoholId))

	@AdminAlcoholsApiDocs.GetCategoryReference
	@GetMapping("/categories/reference")
	fun getCategoryReference(): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminAlcoholQueryService.findAllCategoryReferenceMap())

	@AdminAlcoholsApiDocs.CreateAlcohol
	@PostMapping
	fun createAlcohol(
		@RequestBody @Valid request: AdminAlcoholUpsertRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminAlcoholCommandService.createAlcohol(request))

	@AdminAlcoholsApiDocs.UpdateAlcohol
	@PutMapping("/{alcoholId}")
	fun updateAlcohol(
		@PathVariable alcoholId: Long,
		@RequestBody @Valid request: AdminAlcoholUpsertRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminAlcoholCommandService.updateAlcohol(alcoholId, request))

	@AdminAlcoholsApiDocs.DeleteAlcohol
	@DeleteMapping("/{alcoholId}")
	fun deleteAlcohol(
		@PathVariable alcoholId: Long
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminAlcoholCommandService.deleteAlcohol(alcoholId))
}
