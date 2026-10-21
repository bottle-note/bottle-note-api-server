package app.bottlenote.alcohols.presentation

import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeGroupRequest
import app.bottlenote.alcohols.dto.request.AdminAlcoholCandidateRequest
import app.bottlenote.alcohols.presentation.docs.AdminAlcoholAttributeGroupApiDocs
import app.bottlenote.alcohols.service.AdminAlcoholAttributeGroupService
import app.bottlenote.alcohols.service.AdminAlcoholAttributeSearchService
import app.bottlenote.global.data.response.GlobalResponse
import app.bottlenote.global.service.meta.MetaService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@AdminAlcoholAttributeGroupApiDocs.ApiTag
class AdminAlcoholAttributeGroupController(
	private val service: AdminAlcoholAttributeGroupService,
	private val searchService: AdminAlcoholAttributeSearchService
) {
	@GetMapping("/alcohols/attribute-groups")
	@AdminAlcoholAttributeGroupApiDocs.FindGroups
	fun findGroups(): ResponseEntity<GlobalResponse> = GlobalResponse.ok(service.findAll())

	@PostMapping("/alcohols/attribute-groups")
	@AdminAlcoholAttributeGroupApiDocs.CreateGroup
	fun createGroup(@RequestBody @Valid request: AdminAlcoholAttributeGroupRequest): ResponseEntity<GlobalResponse> = GlobalResponse.ok(service.create(request))

	@PutMapping("/alcohols/attribute-groups/{id}")
	@AdminAlcoholAttributeGroupApiDocs.UpdateGroup
	fun updateGroup(@PathVariable id: Long, @RequestBody @Valid request: AdminAlcoholAttributeGroupRequest): ResponseEntity<GlobalResponse> = GlobalResponse.ok(service.update(id, request))

	@DeleteMapping("/alcohols/attribute-groups/{id}")
	@AdminAlcoholAttributeGroupApiDocs.DeleteGroup
	fun deleteGroup(@PathVariable id: Long): ResponseEntity<GlobalResponse> = GlobalResponse.ok(service.delete(id))

	@PostMapping("/attribute/groups/{groupId}/candidates")
	@AdminAlcoholAttributeGroupApiDocs.FindCandidates
	fun findCandidates(@PathVariable groupId: Long, @RequestBody @Valid request: AdminAlcoholCandidateRequest): ResponseEntity<GlobalResponse> {
		val result = searchService.findCandidates(groupId, request)
		return GlobalResponse.ok(
			result,
			MetaService.createMetaInfo()
				.add("page", request.page())
				.add("size", request.size())
				.add("totalElements", result.totalCount())
				.add("totalPages", (result.totalCount() + request.size() - 1) / request.size())
				.add("hasNext", (request.page().toLong() + 1) * request.size() < result.totalCount())
		)
	}
}
