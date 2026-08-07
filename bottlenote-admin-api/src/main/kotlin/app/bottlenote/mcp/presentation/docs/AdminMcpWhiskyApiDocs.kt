package app.bottlenote.mcp.presentation.docs

import app.bottlenote.mcp.dto.McpWhiskyDetail
import app.bottlenote.mcp.dto.McpWhiskySearchResult
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

/** MCP 게이트웨이 전용 위스키 조회 문서. Admin UI 계약과 분리한다. */
object AdminMcpWhiskyApiDocs {

	@Target(AnnotationTarget.CLASS)
	@Retention(AnnotationRetention.RUNTIME)
	@Tag(name = "MCP", description = "MCP 게이트웨이가 호출하는 위스키 조회 API. 필드·페이징은 에이전트 컨텍스트에 맞게 축소한다")
	annotation class ApiTag

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "MCP용 위스키 목록을 검색한다",
		description = """
keyword, regionId, page, size(최대 50)로 위스키 요약 목록을 조회합니다.
Admin UI /alcohols 와 별도 계약이며, MCP 게이트웨이만 사용합니다.
""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "MCP 위스키 검색 결과",
				content = [Content(schema = Schema(implementation = McpWhiskySearchResult::class))]
			)
		]
	)
	annotation class SearchWhiskies

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "MCP용 위스키 상세를 조회한다",
		description = "alcoholId로 MCP 최적화된 위스키 상세를 조회합니다. 지역·증류소·테이스팅 태그 요약을 포함합니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "MCP 위스키 상세",
				content = [Content(schema = Schema(implementation = McpWhiskyDetail::class))]
			)
		]
	)
	annotation class GetWhiskyDetail
}
