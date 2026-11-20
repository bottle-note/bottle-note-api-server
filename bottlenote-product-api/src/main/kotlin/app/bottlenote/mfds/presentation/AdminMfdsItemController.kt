package app.bottlenote.mfds.presentation

import app.bottlenote.global.data.response.GlobalResponse
import app.bottlenote.mfds.presentation.docs.AdminMfdsDeclarationApiDocs
import app.bottlenote.mfds.service.MfdsDeclarationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/mfds/items")
@AdminMfdsDeclarationApiDocs.ApiTag
class AdminMfdsItemController(
	private val mfdsDeclarationService: MfdsDeclarationService
) {
	@AdminMfdsDeclarationApiDocs.GetLatestItem
	@GetMapping("/{rcno}")
	fun getLatestItem(
		@PathVariable rcno: String
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(mfdsDeclarationService.getLatestItem(rcno))
}
