package app.bottlenote.alcohols.presentation

import app.bottlenote.alcohols.dto.request.AdminScenarioPreviewRequest
import app.bottlenote.alcohols.dto.request.AdminScenarioRequest.*
import app.bottlenote.alcohols.presentation.docs.AdminAlcoholScenarioApiDocs
import app.bottlenote.alcohols.service.AdminAlcoholScenarioService
import app.bottlenote.global.data.response.GlobalResponse
import app.bottlenote.global.service.meta.MetaService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/attribute/groups/{groupId}/scenarios")
@AdminAlcoholScenarioApiDocs.ApiTag
class AdminAlcoholScenarioController(private val service: AdminAlcoholScenarioService) {
	@GetMapping
	@AdminAlcoholScenarioApiDocs.FindAll
	fun findAll(@PathVariable groupId: Long): ResponseEntity<GlobalResponse> = GlobalResponse.ok(service.findAll(groupId))

	@PostMapping
	@AdminAlcoholScenarioApiDocs.Create
	fun create(@PathVariable groupId: Long, @RequestBody @Valid request: Create): ResponseEntity<GlobalResponse> = GlobalResponse.ok(service.create(groupId, request))

	@GetMapping("/{scenarioId}")
	@AdminAlcoholScenarioApiDocs.Detail
	fun getDetail(@PathVariable groupId: Long, @PathVariable scenarioId: Long): ResponseEntity<GlobalResponse> = GlobalResponse.ok(service.getDetail(groupId, scenarioId))

	@PutMapping("/{scenarioId}")
	@AdminAlcoholScenarioApiDocs.Update
	fun update(@PathVariable groupId: Long, @PathVariable scenarioId: Long, @RequestBody @Valid request: Update): ResponseEntity<GlobalResponse> = GlobalResponse.ok(service.update(groupId, scenarioId, request))

	@DeleteMapping("/{scenarioId}")
	@AdminAlcoholScenarioApiDocs.Delete
	fun delete(@PathVariable groupId: Long, @PathVariable scenarioId: Long, @RequestParam expectedLockVersion: Long): ResponseEntity<GlobalResponse> = GlobalResponse.ok(service.delete(groupId, scenarioId, expectedLockVersion))

	@PostMapping("/{scenarioId}/versions")
	@AdminAlcoholScenarioApiDocs.Draft
	fun createDraft(@PathVariable groupId: Long, @PathVariable scenarioId: Long, @RequestBody @Valid request: Draft): ResponseEntity<GlobalResponse> = GlobalResponse.ok(service.createDraft(groupId, scenarioId, request))

	@GetMapping("/{scenarioId}/versions/{versionId}")
	@AdminAlcoholScenarioApiDocs.Version
	fun getVersion(@PathVariable groupId: Long, @PathVariable scenarioId: Long, @PathVariable versionId: Long): ResponseEntity<GlobalResponse> = GlobalResponse.ok(service.getVersion(groupId, scenarioId, versionId))

	@PutMapping("/{scenarioId}/versions/{versionId}")
	@AdminAlcoholScenarioApiDocs.Save
	fun save(@PathVariable groupId: Long, @PathVariable scenarioId: Long, @PathVariable versionId: Long, @RequestBody @Valid request: Save): ResponseEntity<GlobalResponse> = GlobalResponse.ok(service.save(groupId, scenarioId, versionId, request))

	@DeleteMapping("/{scenarioId}/versions/{versionId}")
	@AdminAlcoholScenarioApiDocs.DeleteDraft
	fun deleteDraft(@PathVariable groupId: Long, @PathVariable scenarioId: Long, @PathVariable versionId: Long, @RequestParam expectedScenarioLockVersion: Long, @RequestParam expectedLockVersion: Long): ResponseEntity<GlobalResponse> = GlobalResponse.ok(service.deleteDraft(groupId, scenarioId, versionId, expectedScenarioLockVersion, expectedLockVersion))

	@PostMapping("/{scenarioId}/versions/{versionId}/publish")
	@AdminAlcoholScenarioApiDocs.Publish
	fun publish(@PathVariable groupId: Long, @PathVariable scenarioId: Long, @PathVariable versionId: Long, @RequestBody @Valid request: Publish): ResponseEntity<GlobalResponse> = GlobalResponse.ok(service.publish(groupId, scenarioId, versionId, request))

	@PostMapping("/{scenarioId}/versions/{versionId}/preview")
	@AdminAlcoholScenarioApiDocs.Preview
	fun preview(@PathVariable groupId: Long, @PathVariable scenarioId: Long, @PathVariable versionId: Long, @RequestBody @Valid request: AdminScenarioPreviewRequest): ResponseEntity<GlobalResponse> {
		val result = service.preview(groupId, scenarioId, versionId, request)
		val total = result.candidates().totalCount()
		return GlobalResponse.ok(
			result,
			MetaService.createMetaInfo()
				.add("page", request.page()).add("size", request.size()).add("totalElements", total)
				.add("totalPages", (total + request.size() - 1) / request.size())
				.add("hasNext", (request.page().toLong() + 1) * request.size() < total)
		)
	}
}
