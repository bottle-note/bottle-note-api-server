package app.bottlenote.mfds.presentation.docs

import app.bottlenote.global.dto.response.AdminResultResponse
import app.bottlenote.mfds.dto.response.MfdsRcnoLinkItem
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

/** 수입신고번호(RCNO) 연결 근거 엔드포인트의 문서 설명. */
object AdminMfdsRcnoLinkApiDocs {

	private const val ERROR_SCHEMA = "#/components/schemas/ErrorResponse"

	@Target(AnnotationTarget.CLASS)
	@Retention(AnnotationRetention.RUNTIME)
	@Tag(
		name = "수입 정보",
		description = "식약처 수입 원장에서 수집한 수입사와 수입 신고 데이터를 조회하고, 수입사 연결 근거와 BottleNote 위스키 매칭을 관리한다"
	)
	annotation class ApiTag

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "RCNO 연결 근거 목록을 조회한다",
		description = """
			수입신고번호(rcno) 또는 수입사 ID(importerId)로 연결 근거를 조회합니다.

			rcno를 주면 해당 신고번호의 근거 1건을, importerId를 주면 그 수입사에 연결된 근거 전체를 반환합니다. 두 조건을 모두 주면 rcno가 우선합니다.

			이 원장은 수집기가 적재한 데이터입니다. 수입 신고에 수입사를 연결하거나 해제하는 조작으로는 원장이 바뀌지 않습니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "연결 근거 목록",
				content = [
					Content(
						array = ArraySchema(schema = Schema(implementation = MfdsRcnoLinkItem::class))
					)
				]
			)
		]
	)
	annotation class SearchRcnoLinks

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "RCNO 연결 근거를 등록한다",
		description = """
			수입신고번호와 수입사를 직접 연결하는 근거를 등록합니다. 연결 근거는 MANUAL로 기록됩니다.

			같은 수입신고번호에 이미 근거가 있으면 실패합니다. 기존 근거를 바꾸려면 먼저 삭제한 뒤 다시 등록합니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "등록 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			),
			ApiResponse(
				responseCode = "404",
				description = "요청한 수입사가 없습니다.",
				content = [Content(schema = Schema(ref = ERROR_SCHEMA))]
			),
			ApiResponse(
				responseCode = "409",
				description = "해당 수입신고번호에 이미 연결 근거가 있습니다.",
				content = [Content(schema = Schema(ref = ERROR_SCHEMA))]
			)
		]
	)
	annotation class CreateRcnoLink

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "RCNO 연결 근거를 삭제한다",
		description = "수입신고번호의 연결 근거를 삭제합니다. 근거가 없으면 실패합니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "삭제 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			),
			ApiResponse(
				responseCode = "404",
				description = "해당 수입신고번호의 연결 근거가 없습니다.",
				content = [Content(schema = Schema(ref = ERROR_SCHEMA))]
			)
		]
	)
	annotation class DeleteRcnoLink
}
