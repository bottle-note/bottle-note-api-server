package app.bottlenote.alcohols.presentation

import app.bottlenote.alcohols.dto.request.AdminReferenceSearchRequest
import app.bottlenote.alcohols.dto.request.AdminTastingTagAlcoholRequest
import app.bottlenote.alcohols.dto.request.AdminTastingTagUpsertRequest
import app.bottlenote.alcohols.service.AlcoholReferenceService
import app.bottlenote.alcohols.service.TastingTagService
import app.bottlenote.global.data.response.GlobalResponse
import jakarta.validation.Valid
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
@RequestMapping("/tasting-tags")
class AdminTastingTagController(
	private val alcoholReferenceService: AlcoholReferenceService,
	private val tastingTagService: TastingTagService
) {

	@GetMapping
	fun getAllTastingTags(@ModelAttribute request: AdminReferenceSearchRequest): ResponseEntity<*> {
		return ResponseEntity.ok(alcoholReferenceService.findAllTastingTags(request))
	}

	@GetMapping("/{tagId}")
	fun getTagDetail(@PathVariable tagId: Long): ResponseEntity<*> {
		return GlobalResponse.ok(tastingTagService.getTagDetail(tagId))
	}

	@PostMapping
	fun createTag(@RequestBody @Valid request: AdminTastingTagUpsertRequest): ResponseEntity<*> {
		return GlobalResponse.ok(tastingTagService.createTag(request))
	}

	@PutMapping("/{tagId}")
	fun updateTag(
		@PathVariable tagId: Long,
		@RequestBody @Valid request: AdminTastingTagUpsertRequest
	): ResponseEntity<*> {
		return GlobalResponse.ok(tastingTagService.updateTag(tagId, request))
	}

	@DeleteMapping("/{tagId}")
	fun deleteTag(@PathVariable tagId: Long): ResponseEntity<*> {
		return GlobalResponse.ok(tastingTagService.deleteTag(tagId))
	}

	@PostMapping("/{tagId}/alcohols")
	fun addAlcoholsToTag(
		@PathVariable tagId: Long,
		@RequestBody @Valid request: AdminTastingTagAlcoholRequest
	): ResponseEntity<*> {
		return GlobalResponse.ok(tastingTagService.addAlcoholsToTag(tagId, request.alcoholIds()))
	}

	@DeleteMapping("/{tagId}/alcohols")
	fun removeAlcoholsFromTag(
		@PathVariable tagId: Long,
		@RequestBody @Valid request: AdminTastingTagAlcoholRequest
	): ResponseEntity<*> {
		return GlobalResponse.ok(tastingTagService.removeAlcoholsFromTag(tagId, request.alcoholIds()))
	}
}
