package app.bottlenote.alcohols.presentation

import app.bottlenote.alcohols.dto.request.AdminTastingTagGroupCreateRequest
import app.bottlenote.alcohols.dto.request.AdminTastingTagGroupTagRoleRequest
import app.bottlenote.alcohols.dto.request.AdminTastingTagGroupTagsRequest
import app.bottlenote.alcohols.dto.request.AdminTastingTagGroupUpdateRequest
import app.bottlenote.alcohols.presentation.docs.AdminTastingTagGroupApiDocs
import app.bottlenote.alcohols.service.AdminTastingTagGroupService
import app.bottlenote.global.data.response.GlobalResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/tasting/groups")
@AdminTastingTagGroupApiDocs.ApiTag
class AdminTastingTagGroupController(
	private val adminTastingTagGroupService: AdminTastingTagGroupService
) {
	@AdminTastingTagGroupApiDocs.ListGroups
	@GetMapping
	fun findAllGroups(): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminTastingTagGroupService.findAll())

	@AdminTastingTagGroupApiDocs.GetGroup
	@GetMapping("/{code}")
	fun getGroupDetail(
		@PathVariable code: String
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminTastingTagGroupService.getDetail(code))

	@AdminTastingTagGroupApiDocs.CreateGroup
	@PostMapping
	fun createGroup(
		@RequestBody @Valid request: AdminTastingTagGroupCreateRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminTastingTagGroupService.create(request))

	@AdminTastingTagGroupApiDocs.UpdateGroup
	@PutMapping("/{code}")
	fun updateGroup(
		@PathVariable code: String,
		@RequestBody @Valid request: AdminTastingTagGroupUpdateRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminTastingTagGroupService.update(code, request))

	@AdminTastingTagGroupApiDocs.DeleteGroup
	@DeleteMapping("/{code}")
	fun deleteGroup(
		@PathVariable code: String
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminTastingTagGroupService.delete(code))

	@AdminTastingTagGroupApiDocs.AddTags
	@PostMapping("/{code}/tags")
	fun addGroupTags(
		@PathVariable code: String,
		@RequestBody @Valid request: AdminTastingTagGroupTagsRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminTastingTagGroupService.addTags(code, request))

	@AdminTastingTagGroupApiDocs.ChangeTagRole
	@PutMapping("/{code}/tags/{tagId}")
	fun changeGroupTagRole(
		@PathVariable code: String,
		@PathVariable tagId: Long,
		@RequestBody @Valid request: AdminTastingTagGroupTagRoleRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(
		adminTastingTagGroupService.changeTagRole(code, tagId, request)
	)

	@AdminTastingTagGroupApiDocs.RemoveTag
	@DeleteMapping("/{code}/tags/{tagId}")
	fun removeGroupTag(
		@PathVariable code: String,
		@PathVariable tagId: Long
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminTastingTagGroupService.removeTag(code, tagId))
}
