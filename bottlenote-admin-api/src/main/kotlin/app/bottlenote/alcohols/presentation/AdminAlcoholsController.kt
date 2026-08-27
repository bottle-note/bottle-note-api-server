package app.bottlenote.alcohols.presentation

import app.bottlenote.alcohols.dto.request.AdminAlcoholLookupRequest
import app.bottlenote.alcohols.dto.request.AdminAlcoholSearchRequest
import app.bottlenote.alcohols.dto.request.AdminAlcoholUpsertRequest
import app.bottlenote.alcohols.excel.AdminAlcoholExcelService
import app.bottlenote.alcohols.excel.AlcoholExcelSchema
import app.bottlenote.alcohols.presentation.docs.AdminAlcoholsApiDocs
import app.bottlenote.alcohols.service.AdminAlcoholCommandService
import app.bottlenote.alcohols.service.AdminAlcoholLookupService
import app.bottlenote.alcohols.service.AdminAlcoholQueryService
import app.bottlenote.global.data.response.GlobalResponse
import jakarta.validation.Valid
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/alcohols")
@AdminAlcoholsApiDocs.ApiTag
class AdminAlcoholsController(
	private val adminAlcoholQueryService: AdminAlcoholQueryService,
	private val adminAlcoholCommandService: AdminAlcoholCommandService,
	private val adminAlcoholLookupService: AdminAlcoholLookupService,
	private val adminAlcoholExcelService: AdminAlcoholExcelService,
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

	@AdminAlcoholsApiDocs.DownloadAlcoholExcelTemplate
	@GetMapping("/excel/template")
	fun downloadAlcoholExcelTemplate(): ResponseEntity<ByteArrayResource> {
		val bytes = adminAlcoholExcelService.createTemplateWorkbook()
		val resource = ByteArrayResource(bytes)
		return ResponseEntity.ok()
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${AlcoholExcelSchema.TEMPLATE_FILENAME}\"")
			.contentType(MediaType.parseMediaType(AlcoholExcelSchema.XLSX_CONTENT_TYPE))
			.contentLength(bytes.size.toLong())
			.body(resource)
	}

	@AdminAlcoholsApiDocs.ValidateAlcoholExcel
	@PostMapping("/excel/validate", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
	fun validateAlcoholExcel(
		@RequestPart("file") file: MultipartFile
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminAlcoholExcelService.validate(file))

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
