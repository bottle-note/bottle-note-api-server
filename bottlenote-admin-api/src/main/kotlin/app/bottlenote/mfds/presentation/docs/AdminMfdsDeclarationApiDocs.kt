package app.bottlenote.mfds.presentation.docs

import app.bottlenote.global.dto.response.AdminResultResponse
import app.bottlenote.mfds.dto.response.MfdsDeclarationDetailResponse
import app.bottlenote.mfds.dto.response.MfdsDeclarationListItem
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

/** 식약처 수입 신고 관리 엔드포인트의 문서 설명. */
object AdminMfdsDeclarationApiDocs {

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
		summary = "수입 신고 목록을 조회한다",
		description = """
			정규화 상태, 주류 매칭 결정 근거, 주류 매칭 여부, 연결 수입사, 검색어로 수입 신고 목록을 조회합니다.

			검색어는 한글/영문 제품 검색 키와 수입신고번호에 부분 일치로 적용됩니다. alcoholMatched를 true로 주면 주류가 확정된 신고만, false로 주면 미확정 신고만 반환합니다.

			alcoholMatchDecision에는 아래 값만 지정할 수 있습니다. 정의되지 않은 값은 조용히 무시되지 않고 요청 자체가 실패합니다.

			- CANDIDATE(후보 선택): 자동 매칭이 계산한 후보 목록에서 관리자가 선택한 경우에 해당 값이 사용된다
			- MANUAL(직접 선택): 자동매칭이 아닌 관리자가 직접 선택한 경우에 해당 값이 사용된다
			- AUTO(자동 매칭): 관리자 개입 없이 자동 매칭이 선정한 값이 그대로 확정된 경우에 해당 값이 사용된다


			목록은 ID 내림차순이며 커서 방식으로 페이징합니다. 응답 meta의 nextCursor를 다음 요청의 cursor로 전달하면 다음 페이지를 받을 수 있습니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "수입 신고 목록",
				content = [
					Content(
						array = ArraySchema(schema = Schema(implementation = MfdsDeclarationListItem::class))
					)
				]
			)
		]
	)
	annotation class SearchDeclarations

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "수입 신고 상세 정보를 조회한다",
		description = """
			신고 데이터 ID로 정제 결과 전체와 연결된 수입사 정보, 주류·증류소·지역 매칭 후보 목록을 조회합니다.

			alcoholMatchDecision은 확정된 주류의 선택 근거이며 확정 전이면 비어 있습니다.

			- CANDIDATE(후보 선택): 자동 매칭이 계산한 후보 목록에서 관리자가 선택한 경우에 해당 값이 사용된다
			- MANUAL(직접 선택): 자동매칭이 아닌 관리자가 직접 선택한 경우에 해당 값이 사용된다
			- AUTO(자동 매칭): 관리자 개입 없이 자동 매칭이 선정한 값이 그대로 확정된 경우에 해당 값이 사용된다
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "수입 신고 상세 정보",
				content = [Content(schema = Schema(implementation = MfdsDeclarationDetailResponse::class))]
			)
		]
	)
	annotation class GetDeclarationDetail

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "수입 신고의 정규화 상태를 변경한다",
		description = """
			관리자 검토 결과에 따라 정규화 상태를 변경합니다. 예를 들어 검토가 필요한(REVIEW_REQUIRED) 신고를 확인한 뒤 완료(NORMALIZED) 상태로 전이할 수 있습니다.

			검토자와 검토 메모를 함께 남기면 상세 화면에서 검토 이력으로 확인할 수 있습니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "상태 변경 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			)
		]
	)
	annotation class ChangeNormalizationStatus

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "수입 신고에 수입사를 수동 연결한다",
		description = """
			수집기가 자동으로 연결하지 못한 수입 신고에 수입사를 직접 연결합니다.

			연결 근거는 MANUAL로 기록되며, 같은 수입신고번호(RCNO)의 연결 근거도 함께 남습니다. 이미 수입사가 연결된 신고는 먼저 연결을 해제해야 합니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "연결 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			)
		]
	)
	annotation class LinkImporter

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "수입 신고의 수입사 연결을 해제한다",
		description = "수입 신고에 연결된 수입사를 해제하고, 해당 수입신고번호(RCNO)의 연결 근거도 함께 제거합니다. 연결된 수입사가 없으면 실패합니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "해제 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			)
		]
	)
	annotation class UnlinkImporter
}
