package app.bottlenote.alcohols.presentation

import app.bottlenote.alcohols.dto.request.AdminAlcoholBulkRequest
import app.bottlenote.alcohols.presentation.docs.AdminAlcoholBulkApiDocs
import app.bottlenote.alcohols.service.AdminAlcoholBulkService
import app.bottlenote.global.annotation.SecurityPolicy
import app.bottlenote.global.data.response.GlobalResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/alcohols/bulk")
@SecurityPolicy
@AdminAlcoholBulkApiDocs.ApiTag
class AdminAlcoholBulkController(
	private val adminAlcoholBulkService: AdminAlcoholBulkService
) {
	@PostMapping("/validate")
	@AdminAlcoholBulkApiDocs.ValidateBulk
	fun validate(
		@RequestBody @Valid request: AdminAlcoholBulkRequest
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminAlcoholBulkService.validate(request))

	@PostMapping
	@AdminAlcoholBulkApiDocs.CreateBulk
	fun create(
		@RequestBody @Valid request: AdminAlcoholBulkRequest
	): ResponseEntity<GlobalResponse> {
		val result = adminAlcoholBulkService.create(request)
		return if (result.validation().invalidRows() > 0) {
			ResponseEntity.badRequest().body(GlobalResponse.fail(result.validation()))
		} else {
			GlobalResponse.ok(result)
		}
	}
}
