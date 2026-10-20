package app.bottlenote.alcohols.presentation

import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeGroupRequest
import app.bottlenote.alcohols.presentation.docs.AdminAlcoholAttributeGroupApiDocs
import app.bottlenote.alcohols.service.AdminAlcoholAttributeGroupService
import app.bottlenote.global.data.response.GlobalResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/alcohols/attribute-groups")
@AdminAlcoholAttributeGroupApiDocs.ApiTag
class AdminAlcoholAttributeGroupController(private val service: AdminAlcoholAttributeGroupService) {
	@GetMapping
	@AdminAlcoholAttributeGroupApiDocs.FindGroups
	fun findGroups(): ResponseEntity<GlobalResponse> = GlobalResponse.ok(service.findAll())

	@PostMapping
	@AdminAlcoholAttributeGroupApiDocs.CreateGroup
	fun createGroup(@RequestBody @Valid request: AdminAlcoholAttributeGroupRequest): ResponseEntity<GlobalResponse> = GlobalResponse.ok(service.create(request))

	@PutMapping("/{id}")
	@AdminAlcoholAttributeGroupApiDocs.UpdateGroup
	fun updateGroup(@PathVariable id: Long, @RequestBody @Valid request: AdminAlcoholAttributeGroupRequest): ResponseEntity<GlobalResponse> = GlobalResponse.ok(service.update(id, request))

	@DeleteMapping("/{id}")
	@AdminAlcoholAttributeGroupApiDocs.DeleteGroup
	fun deleteGroup(@PathVariable id: Long): ResponseEntity<GlobalResponse> = GlobalResponse.ok(service.delete(id))
}
