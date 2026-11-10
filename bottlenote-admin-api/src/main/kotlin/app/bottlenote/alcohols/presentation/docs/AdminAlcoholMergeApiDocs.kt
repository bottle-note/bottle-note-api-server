package app.bottlenote.alcohols.presentation.docs

import app.bottlenote.alcohols.dto.response.AdminAlcoholMergeLogResponse
import app.bottlenote.alcohols.dto.response.AdminAlcoholMergePreviewResponse
import app.bottlenote.global.dto.response.AdminResultResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

/** 중복 위스키 병합 엔드포인트의 문서 설명. */
object AdminAlcoholMergeApiDocs {

	@Target(AnnotationTarget.CLASS)
	@Retention(AnnotationRetention.RUNTIME)
	@Tag(name = "알코올")
	annotation class ApiTag

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "중복 위스키 병합을 미리 본다",
		description = """
			기준 위스키 하나에 중복 위스키를 최대 10개까지 합쳤을 때 대상별로 옮겨질 건수(moved)와 겹쳐서 삭제될 건수(discarded)를 보여줍니다. 데이터는 바뀌지 않습니다.

			같은 사용자가 기준과 중복 양쪽에 별점이나 찜을 남겼다면 기준 쪽 기록을 남기고 중복 쪽 기록을 삭제합니다. 검증 규칙은 병합 실행과 같습니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "기준 위스키와 중복 위스키별 이전·삭제 건수",
				content = [Content(schema = Schema(implementation = AdminAlcoholMergePreviewResponse::class))]
			),
			ApiResponse(responseCode = "400", description = "기준 위스키가 삭제됐거나 병합 대상에 기준 위스키가 포함된 경우"),
			ApiResponse(responseCode = "404", description = "기준 또는 중복 위스키를 찾을 수 없는 경우"),
			ApiResponse(responseCode = "409", description = "중복 위스키가 이미 병합된 경우")
		]
	)
	annotation class PreviewMerge

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "중복 위스키를 기준 위스키로 병합한다",
		description = """
			중복 위스키에 달린 리뷰, 별점, 찜, 히스토리, 테이스팅 태그, 속성, 수입 신고 확정 연결을 기준 위스키로 옮기고 중복 위스키를 삭제 처리합니다. 기준 위스키의 이름, 이미지, 설명은 바뀌지 않습니다.

			요청한 중복 위스키 전체를 한 번에 처리하며 하나라도 실패하면 아무것도 바뀌지 않습니다. 병합은 되돌릴 수 없고 결과는 병합 로그에 남습니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "병합 결과. targetId는 기준 위스키 ID",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			),
			ApiResponse(responseCode = "400", description = "기준 위스키가 삭제됐거나 병합 대상에 기준 위스키가 포함된 경우"),
			ApiResponse(responseCode = "404", description = "기준 또는 중복 위스키를 찾을 수 없는 경우"),
			ApiResponse(responseCode = "409", description = "중복 위스키가 이미 병합된 경우")
		]
	)
	annotation class Merge

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "위스키 병합 로그를 조회한다",
		description = """
			병합 기록을 최근 순으로 페이지 단위로 조회합니다. 중복 위스키 한 건이 한 행이며, 한 번의 요청으로 함께 병합된 행은 mergeGroupId가 같습니다.

			기준 위스키나 중복 위스키 ID로 좁힐 수 있고 병합 시점의 이름, 대상별 이전·삭제 건수, 병합한 관리자를 함께 내립니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "병합 로그 목록",
				content = [
					Content(
						array = ArraySchema(schema = Schema(implementation = AdminAlcoholMergeLogResponse::class))
					)
				]
			)
		]
	)
	annotation class ListMergeLogs
}
