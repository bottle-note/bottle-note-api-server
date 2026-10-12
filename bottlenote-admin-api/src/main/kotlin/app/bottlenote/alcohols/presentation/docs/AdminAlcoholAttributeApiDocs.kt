package app.bottlenote.alcohols.presentation.docs

import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeCreateResponse
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeEnumsResponse
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeItem
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeKeyItem
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeKeywordItem
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeSummaryItem
import app.bottlenote.global.dto.response.AdminResultResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

/** 위스키 속성 어드민 엔드포인트의 문서 설명. */
object AdminAlcoholAttributeApiDocs {

	@Target(AnnotationTarget.CLASS)
	@Retention(AnnotationRetention.RUNTIME)
	@Tag(name = "알코올")
	annotation class ApiTag

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "선택한 위스키의 속성을 조회한다",
		description = """
			alcoholIds로 고른 위스키들의 속성 행을 모두 반환합니다.

			alcoholIds는 쉼표로 구분합니다. 비어 있으면 실패합니다.
			"""
	)
	@ApiResponse(
		responseCode = "200",
		description = "속성 목록",
		content = [
			Content(array = ArraySchema(schema = Schema(implementation = AdminAlcoholAttributeItem::class)))
		]
	)
	annotation class FindAttributes

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "선택한 위스키의 속성을 묶어 조회한다",
		description = """
			네 병 이상 비교 화면용 집계입니다.

			숫자 형식은 항목(key)별로, 그 외는 항목과 값별로 묶습니다. 숫자 묶음은 최소·최대와 값이 모두 같은지 함께 반환합니다.
			"""
	)
	@ApiResponse(
		responseCode = "200",
		description = "속성 묶음",
		content = [
			Content(array = ArraySchema(schema = Schema(implementation = AdminAlcoholAttributeSummaryItem::class)))
		]
	)
	annotation class SummarizeAttributes

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "여러 위스키에 속성을 추가한다",
		description = """
			각 위스키에 같은 속성을 추가합니다.

			같은 위스키에 같은 항목·값이 이미 있으면 그 위스키는 건너뜁니다. 없는 위스키가 있으면 실패합니다.
			"""
	)
	@ApiResponse(
		responseCode = "200",
		description = "추가 결과",
		content = [Content(schema = Schema(implementation = AdminAlcoholAttributeCreateResponse::class))]
	)
	annotation class CreateAttributes

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "위스키 속성을 수정한다",
		description = "분류, 값 형식, 값, 근거, 기준 시점을 수정합니다. 항목(key)은 바꾸지 않습니다."
	)
	@ApiResponse(
		responseCode = "200",
		description = "수정 처리 결과",
		content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
	)
	annotation class UpdateAttribute

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "위스키 속성을 삭제한다", description = "속성 한 건을 삭제합니다.")
	@ApiResponse(
		responseCode = "200",
		description = "삭제 처리 결과",
		content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
	)
	annotation class DeleteAttribute

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "위스키 속성을 여러 건 삭제한다",
		description = "요청한 ID 중 실제로 지운 속성 행을 그대로 돌려줍니다. 실행을 취소할 때 이 응답을 다시 넣으면 됩니다."
	)
	@ApiResponse(
		responseCode = "200",
		description = "삭제된 속성 목록",
		content = [
			Content(array = ArraySchema(schema = Schema(implementation = AdminAlcoholAttributeItem::class)))
		]
	)
	annotation class DeleteAttributes

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "속성 항목별 집계를 조회한다",
		description = "전체 속성에서 항목·분류·값 형식별 건수와 위스키 수를 반환합니다."
	)
	@ApiResponse(
		responseCode = "200",
		description = "항목 집계",
		content = [
			Content(array = ArraySchema(schema = Schema(implementation = AdminAlcoholAttributeKeyItem::class)))
		]
	)
	annotation class FindAttributeKeys

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "키워드 속성 값을 조회한다",
		description = "분류가 KEYWORD인 값을 중복 없이, 사용 위스키가 많은 순으로 반환합니다."
	)
	@ApiResponse(
		responseCode = "200",
		description = "키워드 목록",
		content = [
			Content(array = ArraySchema(schema = Schema(implementation = AdminAlcoholAttributeKeywordItem::class)))
		]
	)
	annotation class FindAttributeKeywords

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "속성 화면 라벨을 조회한다",
		description = "분류, 값 형식, 값 제공자 코드를 한글 이름·영문 이름·설명과 함께 반환합니다."
	)
	@ApiResponse(
		responseCode = "200",
		description = "속성 코드 목록",
		content = [Content(schema = Schema(implementation = AdminAlcoholAttributeEnumsResponse::class))]
	)
	annotation class FindAttributeEnums
}
