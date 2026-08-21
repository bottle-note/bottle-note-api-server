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

	@Target(AnnotationTarget.CLASS)
	@Retention(AnnotationRetention.RUNTIME)
	@Tag(name = "식약처 RCNO 연결 근거", description = "수입신고번호별 수입사 연결 근거를 조회하고 등록·삭제한다")
	annotation class ApiTag

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "RCNO 연결 근거 목록을 조회한다",
		description = """
			수입신고번호(rcno) 또는 수입사 ID(importerId)로 연결 근거를 조회합니다.

			rcno를 주면 해당 신고번호의 근거 1건을, importerId를 주면 그 수입사에 연결된 근거 전체를 반환합니다. 두 조건을 모두 주면 rcno가 우선합니다.
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
			)
		]
	)
	annotation class DeleteRcnoLink
}
