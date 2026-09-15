package app.bottlenote.campaigncontent.presentation

import app.bottlenote.campaigncontent.dto.request.AdminCampaignContentCreateRequest
import app.bottlenote.campaigncontent.dto.request.AdminCampaignContentMetricsRequest
import app.bottlenote.campaigncontent.dto.request.AdminCampaignContentSearchRequest
import app.bottlenote.campaigncontent.dto.request.AdminCampaignContentStatusRequest
import app.bottlenote.campaigncontent.dto.request.AdminCampaignContentUpdateRequest
import app.bottlenote.campaigncontent.presentation.docs.AdminCampaignContentApiDocs
import app.bottlenote.campaigncontent.service.AdminCampaignContentService
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
@RequestMapping("/campaign-contents")
@AdminCampaignContentApiDocs.ApiTag
class AdminCampaignContentController(
	private val adminCampaignContentService: AdminCampaignContentService
) {
	@AdminCampaignContentApiDocs.GetCampaignContents
	@GetMapping
	fun list(
		@ModelAttribute request: AdminCampaignContentSearchRequest
	): ResponseEntity<GlobalResponse> = ResponseEntity.ok(adminCampaignContentService.search(request))

	@AdminCampaignContentApiDocs.GetCampaignContentDetail
	@GetMapping("/{campaignContentId}")
	fun detail(
		@PathVariable campaignContentId: Long
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminCampaignContentService.getDetail(campaignContentId))

	@AdminCampaignContentApiDocs.CreateCampaignContent
	@PostMapping
	fun create(
		@RequestBody @Valid request: AdminCampaignContentCreateRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminCampaignContentService.create(request))

	@AdminCampaignContentApiDocs.UpdateCampaignContent
	@PutMapping("/{campaignContentId}")
	fun update(
		@PathVariable campaignContentId: Long,
		@RequestBody @Valid request: AdminCampaignContentUpdateRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminCampaignContentService.update(campaignContentId, request))

	@AdminCampaignContentApiDocs.DeleteCampaignContent
	@DeleteMapping("/{campaignContentId}")
	fun delete(
		@PathVariable campaignContentId: Long
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminCampaignContentService.delete(campaignContentId))

	@AdminCampaignContentApiDocs.UpdateCampaignContentStatus
	@PatchMapping("/{campaignContentId}/status")
	fun updateStatus(
		@PathVariable campaignContentId: Long,
		@RequestBody @Valid request: AdminCampaignContentStatusRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminCampaignContentService.updateStatus(campaignContentId, request))

	@AdminCampaignContentApiDocs.GetCampaignContentMetrics
	@GetMapping("/{campaignContentId}/metrics")
	fun metrics(
		@PathVariable campaignContentId: Long,
		@ModelAttribute request: AdminCampaignContentMetricsRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminCampaignContentService.getMetrics(campaignContentId, request))
}
