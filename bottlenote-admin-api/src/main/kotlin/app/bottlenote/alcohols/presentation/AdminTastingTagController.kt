package app.bottlenote.alcohols.presentation

import app.bottlenote.alcohols.dto.request.AdminReferenceSearchRequest
import app.bottlenote.alcohols.dto.request.AdminTastingTagAlcoholRequest
import app.bottlenote.alcohols.dto.request.AdminTastingTagUpsertRequest
import app.bottlenote.alcohols.presentation.docs.AdminTastingTagApiDocs
import app.bottlenote.alcohols.service.AlcoholReferenceService
import app.bottlenote.alcohols.service.TastingTagService
import app.bottlenote.global.data.response.GlobalResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/tasting-tags")
@AdminTastingTagApiDocs.ApiTag
class AdminTastingTagController(
	private val alcoholReferenceService: AlcoholReferenceService,
	private val tastingTagService: TastingTagService
) {
	@AdminTastingTagApiDocs.GetAllTastingTags
	@GetMapping
	fun getAllTastingTags(
		@ModelAttribute request: AdminReferenceSearchRequest
	): ResponseEntity<GlobalResponse> = ResponseEntity.ok(alcoholReferenceService.findAllTastingTags(request))

	@AdminTastingTagApiDocs.GetTastingTagDetail
	@GetMapping("/{tagId}")
	fun getTagDetail(
		@PathVariable tagId: Long
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(tastingTagService.getTagDetail(tagId))

	@AdminTastingTagApiDocs.CreateTastingTag
	@PostMapping
	fun createTag(
		@RequestBody @Valid request: AdminTastingTagUpsertRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(tastingTagService.createTag(request))

	@AdminTastingTagApiDocs.UpdateTastingTag
	@PutMapping("/{tagId}")
	fun updateTag(
		@PathVariable tagId: Long,
		@RequestBody @Valid request: AdminTastingTagUpsertRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(tastingTagService.updateTag(tagId, request))

	@AdminTastingTagApiDocs.DeleteTastingTag
	@DeleteMapping("/{tagId}")
	fun deleteTag(
		@PathVariable tagId: Long
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(tastingTagService.deleteTag(tagId))

	@AdminTastingTagApiDocs.AddAlcoholsToTastingTag
	@PostMapping("/{tagId}/alcohols")
	fun addAlcoholsToTag(
		@PathVariable tagId: Long,
		@RequestBody @Valid request: AdminTastingTagAlcoholRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(tastingTagService.addAlcoholsToTag(tagId, request.alcoholIds()))

	@AdminTastingTagApiDocs.RemoveAlcoholsFromTastingTag
	@DeleteMapping("/{tagId}/alcohols")
	fun removeAlcoholsFromTag(
		@PathVariable tagId: Long,
		@RequestBody @Valid request: AdminTastingTagAlcoholRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(tastingTagService.removeAlcoholsFromTag(tagId, request.alcoholIds()))
}
