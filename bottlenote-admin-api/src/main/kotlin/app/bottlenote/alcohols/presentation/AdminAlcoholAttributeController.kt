package app.bottlenote.alcohols.presentation

import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeBatchDeleteRequest
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeCreateRequest
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeSearchRequest
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeUpdateRequest
import app.bottlenote.alcohols.dto.request.AdminAttributedAlcoholSearchRequest
import app.bottlenote.alcohols.presentation.docs.AdminAlcoholAttributeApiDocs
import app.bottlenote.alcohols.service.AdminAlcoholAttributeService
import app.bottlenote.global.data.response.GlobalResponse
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/alcohols/attributes")
@AdminAlcoholAttributeApiDocs.ApiTag
class AdminAlcoholAttributeController(
	private val adminAlcoholAttributeService: AdminAlcoholAttributeService
) {
	@AdminAlcoholAttributeApiDocs.FindAttributes
	@GetMapping
	fun findAttributes(
		@ParameterObject @ModelAttribute @Valid request: AdminAlcoholAttributeSearchRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminAlcoholAttributeService.findAll(request))

	@AdminAlcoholAttributeApiDocs.SummarizeAttributes
	@GetMapping("/summary")
	fun summarizeAttributes(
		@ParameterObject @ModelAttribute @Valid request: AdminAlcoholAttributeSearchRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminAlcoholAttributeService.summarize(request))

	@AdminAlcoholAttributeApiDocs.FindAttributedAlcohols
	@GetMapping("/alcohols")
	fun findAttributedAlcohols(
		@ParameterObject @ModelAttribute @Valid request: AdminAttributedAlcoholSearchRequest
	): ResponseEntity<GlobalResponse> = ResponseEntity.ok(GlobalResponse.fromPage(adminAlcoholAttributeService.searchAlcohols(request)))

	@AdminAlcoholAttributeApiDocs.CreateAttributes
	@PostMapping
	fun createAttributes(
		@RequestBody @Valid request: AdminAlcoholAttributeCreateRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminAlcoholAttributeService.create(request))

	@AdminAlcoholAttributeApiDocs.UpdateAttribute
	@PutMapping("/{id}")
	fun updateAttribute(
		@PathVariable id: Long,
		@RequestBody @Valid request: AdminAlcoholAttributeUpdateRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminAlcoholAttributeService.update(id, request))

	@AdminAlcoholAttributeApiDocs.DeleteAttribute
	@DeleteMapping("/{id}")
	fun deleteAttribute(
		@PathVariable id: Long
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminAlcoholAttributeService.delete(id))

	@AdminAlcoholAttributeApiDocs.DeleteAttributes
	@PostMapping("/batch/delete")
	fun deleteAttributes(
		@RequestBody @Valid request: AdminAlcoholAttributeBatchDeleteRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminAlcoholAttributeService.deleteBatch(request))

	@AdminAlcoholAttributeApiDocs.FindAttributeKeys
	@GetMapping("/keys")
	fun findAttributeKeys(): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminAlcoholAttributeService.findKeys())

	@AdminAlcoholAttributeApiDocs.FindAttributeKeywords
	@GetMapping("/keywords")
	fun findAttributeKeywords(): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminAlcoholAttributeService.findKeywords())

	@AdminAlcoholAttributeApiDocs.FindAttributeEnums
	@GetMapping("/enums")
	fun findAttributeEnums(): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminAlcoholAttributeService.findEnums())
}
