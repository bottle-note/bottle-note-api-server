package app.bottlenote.alcohols.presentation.docs

import app.bottlenote.alcohols.dto.response.AdminTastingTagCatalogItem
import app.bottlenote.alcohols.dto.response.AdminTastingTagIconResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

/** 아이콘을 뺀 테이스팅 태그 사전 조회 문서. */
object AdminTastingTagCatalogApiDocs {

	@Target(AnnotationTarget.CLASS)
	@Retention(AnnotationRetention.RUNTIME)
	@Tag(name = "테이스팅 태그")
	annotation class ApiTag

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "아이콘 없는 테이스팅 태그 전체를 조회한다",
		description = """
			페이지 없이 전체 태그를 한글 이름 순으로 반환합니다.
			아이콘은 포함하지 않으며, 각 태그의 위스키 수와 소속 맛 그룹을 함께 반환합니다.
			"""
	)
	@ApiResponse(
		responseCode = "200",
		description = "아이콘을 뺀 전체 테이스팅 태그",
		content = [
			Content(array = ArraySchema(schema = Schema(implementation = AdminTastingTagCatalogItem::class)))
		]
	)
	annotation class GetAllTags

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "테이스팅 태그 아이콘을 조회한다",
		description = "태그 하나의 base64 아이콘만 반환합니다. 아이콘이 없으면 icon은 null입니다."
	)
	@ApiResponse(
		responseCode = "200",
		description = "테이스팅 태그 아이콘",
		content = [Content(schema = Schema(implementation = AdminTastingTagIconResponse::class))]
	)
	@ApiResponse(responseCode = "404", description = "테이스팅 태그가 없습니다")
	annotation class GetIcon

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "맛 그룹에 속하지 않은 테이스팅 태그를 조회한다",
		description = """
			어느 맛 그룹에도 들어가지 않은 태그를 위스키 수가 많은 순으로 반환합니다.
			keyword가 있으면 한글 이름이 그 문자열을 포함하는 태그만 반환합니다.
			"""
	)
	@ApiResponse(
		responseCode = "200",
		description = "맛 그룹에 속하지 않은 테이스팅 태그",
		content = [
			Content(array = ArraySchema(schema = Schema(implementation = AdminTastingTagCatalogItem::class)))
		]
	)
	annotation class GetUngrouped
}
