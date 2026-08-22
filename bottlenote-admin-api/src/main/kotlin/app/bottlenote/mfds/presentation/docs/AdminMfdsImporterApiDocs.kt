package app.bottlenote.mfds.presentation.docs

import app.bottlenote.global.dto.response.AdminResultResponse
import app.bottlenote.mfds.dto.response.MfdsImporterItem
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

/** 식약처 수입사 관리 엔드포인트의 문서 설명. */
object AdminMfdsImporterApiDocs {

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
		summary = "수입사 목록을 조회한다",
		description = """
			관리 상태와 검색어로 수입사 목록을 조회합니다. 검색어는 수입사명, 인허가 번호, 공식 업소 코드에 부분 일치로 적용됩니다.

			목록은 ID 내림차순이며 커서 방식으로 페이징합니다. 응답 meta의 nextCursor를 다음 요청의 cursor로 전달하면 다음 페이지를 받을 수 있고, hasNext가 false면 마지막 페이지입니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "수입사 목록",
				content = [
					Content(
						array = ArraySchema(schema = Schema(implementation = MfdsImporterItem::class))
					)
				]
			)
		]
	)
	annotation class SearchImporters

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "수입사 상세 정보를 조회한다",
		description = "수입사 ID로 공식 정보, 관리자 설명·메모, 관리 상태를 포함한 상세 정보를 조회합니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "수입사 상세 정보",
				content = [Content(schema = Schema(implementation = MfdsImporterItem::class))]
			),
			ApiResponse(
				responseCode = "404",
				description = "요청한 수입사가 없습니다.",
				content = [Content(schema = Schema(ref = ERROR_SCHEMA))]
			)
		]
	)
	annotation class GetImporterDetail

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "수입사를 수동 등록한다",
		description = """
			수집기가 아직 확보하지 못한 수입사를 관리자가 직접 등록합니다.

			공식 업소 코드가 이미 등록된 수입사와 중복되면 실패합니다. 등록 근거가 된 공식 목록 조회 URL을 반드시 함께 남깁니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "등록 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			),
			ApiResponse(
				responseCode = "409",
				description = "동일한 공식 업소 코드의 수입사가 이미 있습니다.",
				content = [Content(schema = Schema(ref = ERROR_SCHEMA))]
			)
		]
	)
	annotation class CreateImporter

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "수입사 관리 항목을 수정한다",
		description = "수입사의 공식명, 공개 설명, 관리자 메모, 관리 상태(ACTIVE/INACTIVE)를 수정합니다. 공식명을 바꾸면 자동 매칭 키도 함께 갱신됩니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "수정 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			),
			ApiResponse(
				responseCode = "404",
				description = "요청한 수입사가 없습니다.",
				content = [Content(schema = Schema(ref = ERROR_SCHEMA))]
			)
		]
	)
	annotation class UpdateImporter

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "수입사를 삭제한다",
		description = "수입사를 삭제합니다. 연결된 수입 신고나 수입신고번호 연결 근거가 하나라도 있으면 삭제할 수 없습니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "삭제 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			),
			ApiResponse(
				responseCode = "404",
				description = "요청한 수입사가 없습니다.",
				content = [Content(schema = Schema(ref = ERROR_SCHEMA))]
			),
			ApiResponse(
				responseCode = "409",
				description = "연결된 수입 신고나 수입신고번호 원장이 있어 삭제할 수 없습니다.",
				content = [Content(schema = Schema(ref = ERROR_SCHEMA))]
			)
		]
	)
	annotation class DeleteImporter
}
