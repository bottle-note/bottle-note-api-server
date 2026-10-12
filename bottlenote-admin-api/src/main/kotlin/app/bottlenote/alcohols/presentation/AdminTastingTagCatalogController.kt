package app.bottlenote.alcohols.presentation

import app.bottlenote.alcohols.presentation.docs.AdminTastingTagCatalogApiDocs
import app.bottlenote.alcohols.service.AdminTastingTagCatalogService
import app.bottlenote.global.data.response.GlobalResponse
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/tasting/tags")
@AdminTastingTagCatalogApiDocs.ApiTag
class AdminTastingTagCatalogController(
	private val adminTastingTagCatalogService: AdminTastingTagCatalogService
) {
	@AdminTastingTagCatalogApiDocs.GetAllTags
	@GetMapping("/all")
	fun findAll(): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminTastingTagCatalogService.findAll())

	@AdminTastingTagCatalogApiDocs.GetIcon
	@GetMapping("/{id}/icon")
	fun findIcon(
		@PathVariable id: Long
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminTastingTagCatalogService.findIcon(id))

	@AdminTastingTagCatalogApiDocs.GetUngrouped
	@GetMapping("/ungrouped")
	fun findUngrouped(
		@Parameter(description = "한글 이름 부분 일치. 비우면 미소속 태그 전체를 반환합니다.")
		@RequestParam(required = false) keyword: String?
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminTastingTagCatalogService.findUngrouped(keyword))
}
