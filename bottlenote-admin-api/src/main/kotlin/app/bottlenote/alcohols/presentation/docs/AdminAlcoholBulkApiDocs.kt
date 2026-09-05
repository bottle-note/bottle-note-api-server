package app.bottlenote.alcohols.presentation.docs

import app.bottlenote.alcohols.dto.response.AdminAlcoholBulkCreateResponse
import app.bottlenote.alcohols.dto.response.AdminAlcoholBulkValidateResponse
import app.bottlenote.global.data.response.Error
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

object AdminAlcoholBulkApiDocs {
	@Target(AnnotationTarget.CLASS)
	@Retention(AnnotationRetention.RUNTIME)
	@Tag(name = "알코올 벌크", description = "엑셀과 JSON의 공통 검증 및 일괄 등록")
	annotation class ApiTag

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "알코올 JSON 목록을 검증한다",
		description = "최대 1,000행을 저장 없이 검증한다. clientRowId는 요청 안에서 유일해야 한다. 오류 없는 행의 normalized는 벌크 저장 입력으로 재사용할 수 있다. 중복 후보와 데이터 불일치는 경고이며 자동 병합하지 않는다. 필수 항목은 clientRowId, korName, engName, abv, type, korCategory, engCategory, regionId, distilleryId, volume이다. type은 WHISKY/RUM/VODKA/GIN/TEQUILA/BRANDY/BEER/WINE/ETC 또는 한글 표시값이다. categoryGroup은 SINGLE_MALT/BLEND/BLENDED_MALT/BOURBON/RYE/OTHER 또는 한글 표시값이며, 생략 시 카테고리로 유일하게 추론하거나 비위스키에 OTHER를 사용한다. age/cask/description/tastingTagIds/imageUrl은 선택이다."
	)
	@ApiResponse(responseCode = "200", description = "행별 오류·경고·정규화 결과", content = [Content(schema = Schema(implementation = ValidateEnvelope::class))])
	@ApiResponse(responseCode = "400", description = "잘못된 JSON, 빈 목록 또는 최대 행 수 초과", content = [Content(schema = Schema(implementation = RequestFailureEnvelope::class))])
	@ApiResponse(responseCode = "401", description = "관리자 인증이 없는 경우")
	annotation class ValidateBulk

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "알코올 목록을 일괄 등록한다",
		description = "엑셀 검증 결과 또는 직접 작성한 JSON rows를 다시 검증한 뒤 하나의 트랜잭션으로 등록한다. 오류가 있으면 전혀 저장하지 않고 400 errors에 검증 결과를 반환한다. 경고만 있으면 모두 등록하며 중복 병합은 하지 않는다. 반복 POST는 별도 등록 요청이므로 자동 재시도하지 않는다. 이미지는 선택이며 기존 업로드의 viewUrl을 사용한다."
	)
	@ApiResponse(responseCode = "200", description = "등록 건수와 clientRowId별 생성 ID", content = [Content(schema = Schema(implementation = CreateEnvelope::class))])
	@ApiResponse(responseCode = "400", description = "행 검증 실패 또는 잘못된 요청 목록", content = [Content(schema = Schema(oneOf = [ValidationFailureEnvelope::class, RequestFailureEnvelope::class]))])
	@ApiResponse(responseCode = "401", description = "관리자 인증이 없는 경우")
	annotation class CreateBulk

	@Schema(name = "AlcoholBulkValidateEnvelope")
	data class ValidateEnvelope(
		val success: Boolean,
		val code: Int,
		val data: AdminAlcoholBulkValidateResponse,
		val errors: List<Any> = emptyList(),
		val meta: Map<String, Any?> = emptyMap()
	)

	@Schema(name = "AlcoholBulkCreateEnvelope")
	data class CreateEnvelope(
		val success: Boolean,
		val code: Int,
		val data: AdminAlcoholBulkCreateResponse,
		val errors: List<Any> = emptyList(),
		val meta: Map<String, Any?> = emptyMap()
	)

	@Schema(name = "AlcoholBulkValidationFailureEnvelope")
	data class ValidationFailureEnvelope(
		val success: Boolean,
		val code: Int,
		val data: List<Any> = emptyList(),
		val errors: AdminAlcoholBulkValidateResponse,
		val meta: Map<String, Any?> = emptyMap()
	)

	@Schema(name = "AlcoholBulkRequestFailureEnvelope")
	data class RequestFailureEnvelope(
		val success: Boolean,
		val code: Int,
		val data: List<Any> = emptyList(),
		val errors: List<Error>,
		val meta: Map<String, Any?> = emptyMap()
	)
}
