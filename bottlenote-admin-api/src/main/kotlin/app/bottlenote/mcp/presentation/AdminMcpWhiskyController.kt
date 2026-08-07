package app.bottlenote.mcp.presentation

import app.bottlenote.global.data.response.GlobalResponse
import app.bottlenote.mcp.dto.McpWhiskySearchRequest
import app.bottlenote.mcp.presentation.docs.AdminMcpWhiskyApiDocs
import app.bottlenote.mcp.service.AdminMcpWhiskyService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * MCP-optimized admin whisky APIs.
 *
 * Consumed only by the MCP gateway (not public admin UI contract).
 * Full path: /admin/api/v1/mcp/whiskies
 */
@RestController
@RequestMapping("/mcp/whiskies")
@AdminMcpWhiskyApiDocs.ApiTag
class AdminMcpWhiskyController(
	private val adminMcpWhiskyService: AdminMcpWhiskyService
) {
	@AdminMcpWhiskyApiDocs.SearchWhiskies
	@GetMapping
	fun search(
		@RequestParam(required = false) keyword: String?,
		@RequestParam(required = false) regionId: Long?,
		@RequestParam(required = false) page: Int?,
		@RequestParam(required = false) size: Int?
	): ResponseEntity<GlobalResponse> {
		val result =
			adminMcpWhiskyService.search(
				McpWhiskySearchRequest(
					keyword = keyword,
					regionId = regionId,
					page = page,
					size = size
				)
			)
		return GlobalResponse.ok(result)
	}

	@AdminMcpWhiskyApiDocs.GetWhiskyDetail
	@GetMapping("/{alcoholId}")
	fun getDetail(
		@PathVariable alcoholId: Long
	): ResponseEntity<GlobalResponse> = GlobalResponse.ok(adminMcpWhiskyService.getDetail(alcoholId))
}
