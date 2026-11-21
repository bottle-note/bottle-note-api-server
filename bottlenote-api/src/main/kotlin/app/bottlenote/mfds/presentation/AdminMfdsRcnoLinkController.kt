package app.bottlenote.mfds.presentation

import app.bottlenote.global.data.response.GlobalResponse
import app.bottlenote.mfds.dto.request.MfdsRcnoLinkCreateRequest
import app.bottlenote.mfds.presentation.docs.AdminMfdsRcnoLinkApiDocs
import app.bottlenote.mfds.service.MfdsRcnoLinkService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/mfds/rcno-links")
@AdminMfdsRcnoLinkApiDocs.ApiTag
class AdminMfdsRcnoLinkController(
	private val mfdsRcnoLinkService: MfdsRcnoLinkService
) {
	@AdminMfdsRcnoLinkApiDocs.SearchRcnoLinks
	@GetMapping
	fun searchRcnoLinks(
		@RequestParam(required = false) rcno: String?,
		@RequestParam(required = false) importerId: Long?
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(mfdsRcnoLinkService.search(rcno, importerId))

	@AdminMfdsRcnoLinkApiDocs.CreateRcnoLink
	@PostMapping
	fun createRcnoLink(
		@RequestBody @Valid request: MfdsRcnoLinkCreateRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(mfdsRcnoLinkService.create(request))

	@AdminMfdsRcnoLinkApiDocs.DeleteRcnoLink
	@DeleteMapping("/{rcno}")
	fun deleteRcnoLink(
		@PathVariable rcno: String
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(mfdsRcnoLinkService.delete(rcno))
}
