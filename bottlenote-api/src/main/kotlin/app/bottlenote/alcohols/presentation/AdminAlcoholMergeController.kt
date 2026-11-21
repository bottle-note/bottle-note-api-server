package app.bottlenote.alcohols.presentation

import app.bottlenote.alcohols.dto.request.AdminAlcoholMergeLogSearchRequest
import app.bottlenote.alcohols.dto.request.AdminAlcoholMergeRequest
import app.bottlenote.alcohols.presentation.docs.AdminAlcoholMergeApiDocs
import app.bottlenote.alcohols.service.AdminAlcoholMergeService
import app.bottlenote.global.data.response.GlobalResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/alcohols/merges")
@AdminAlcoholMergeApiDocs.ApiTag
class AdminAlcoholMergeController(
	private val adminAlcoholMergeService: AdminAlcoholMergeService
) {
	@AdminAlcoholMergeApiDocs.PreviewMerge
	@PostMapping("/preview")
	fun preview(
		@RequestBody @Valid request: AdminAlcoholMergeRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminAlcoholMergeService.preview(request))

	@AdminAlcoholMergeApiDocs.Merge
	@PostMapping
	fun merge(
		@RequestBody @Valid request: AdminAlcoholMergeRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminAlcoholMergeService.merge(request))

	@AdminAlcoholMergeApiDocs.ListMergeLogs
	@GetMapping
	fun listLogs(
		@ModelAttribute @Valid request: AdminAlcoholMergeLogSearchRequest
	): ResponseEntity<GlobalResponse> = ResponseEntity.ok(adminAlcoholMergeService.searchLogs(request))
}
