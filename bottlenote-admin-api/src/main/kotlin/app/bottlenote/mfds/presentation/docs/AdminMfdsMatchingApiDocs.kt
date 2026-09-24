package app.bottlenote.mfds.presentation.docs

import app.bottlenote.mfds.dto.response.MfdsBulkMatchingConfirmResponse
import app.bottlenote.mfds.dto.response.MfdsBulkMatchingPreviewResponse
import app.bottlenote.mfds.dto.response.MfdsMatchingCandidatesResponse
import app.bottlenote.mfds.dto.response.MfdsMatchingConfirmResponse
import app.bottlenote.mfds.dto.response.MfdsMatchingRunResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

/** MFDS 수입 원장 매칭 엔드포인트의 문서 설명. */
object AdminMfdsMatchingApiDocs {

	private const val ERROR_SCHEMA = "#/components/schemas/ErrorResponse"

	@Target(AnnotationTarget.CLASS)
	@Retention(AnnotationRetention.RUNTIME)
	@Tag(name = "수입 정보")
	annotation class ApiTag

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "매칭을 실행한다",
		description = """
			신고 정제 데이터 한 건을 전체 위스키·증류소·지역과 비교해 유사도 점수를 계산합니다.

			점수 0.4 이상인 후보 중 위스키는 상위 10개, 증류소·지역은 각각 상위 3개를 저장하고, 각 후보의 요소별 점수 근거(이름·도수·숙성·카테고리·지역)와 함께 반환합니다.
			실행 이력과 후보를 mfds_matching_runs / mfds_matching_candidates에 저장하고 신고의 최신 실행을 갱신합니다. 이전 실행의 후보는 보존합니다.
			브랜드·제품명·숙성·배치·캐스크·연도 비교 근거와 reviewRequired를 반환합니다. v2 점수는 0~1 범위, 소수점 4자리입니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "계산된 후보 목록과 점수 근거",
				content = [Content(schema = Schema(implementation = MfdsMatchingRunResponse::class))]
			),
			ApiResponse(
				responseCode = "404",
				description = "요청한 수입 신고가 없습니다.",
				content = [Content(schema = Schema(ref = ERROR_SCHEMA))]
			)
		]
	)
	annotation class RunMatching

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "저장된 매칭 후보를 조회한다",
		description = """
			신고의 matching_run_id와 matching_version이 일치하는 완료 실행의 후보와 현재 확정 상태를 조회합니다. 고정 후보 컬럼은 참조하지 않습니다.
			유효한 실행이 없으면 빈 배열이며, 다시 계산해야 합니다. Admin v2 실행은 저장된 점수 근거도 반환합니다. 이전 수집기 실행의 점수는 원래 척도를 유지하며 scoreDetail은 null입니다.

			확정 상태(selection)의 alcoholMatchDecision, distilleryMatchSource, regionMatchSource는 저장된 값을 그대로 돌려줍니다. 관리자가 확정하기 전이라도 정규화 배치가 남긴 판정 값이 들어 있을 수 있습니다.

			관리자 확정으로 기록되는 값
			- CANDIDATE(후보 선택): 자동 매칭이 계산한 후보 목록에서 관리자가 선택한 경우에 해당 값이 사용된다
			- MANUAL(직접 선택): 자동매칭이 아닌 관리자가 직접 선택한 경우에 해당 값이 사용된다

			정규화 배치가 남기는 값
			- AUTO_SELECTED: 배치가 단일 후보를 자동 선정했다
			- NO_MATCH: 후보를 찾지 못했다
			- REVIEW / AMBIGUOUS / CONFLICT_REVIEW: 사람이 판단해야 하는 상태다

			확정 여부는 selectedAlcoholId 등 선택 ID가 채워졌는지로 판단하십시오.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "저장된 후보와 확정 상태",
				content = [Content(schema = Schema(implementation = MfdsMatchingCandidatesResponse::class))]
			),
			ApiResponse(
				responseCode = "404",
				description = "요청한 수입 신고가 없습니다.",
				content = [Content(schema = Schema(ref = ERROR_SCHEMA))]
			)
		]
	)
	annotation class GetCandidates

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "매칭을 확정한다",
		description = """
			신고 정제 데이터에 위스키를 확정 연결합니다. 증류소와 지역은 선택 사항입니다.

			후보 목록에 있는 ID를 고르면 CANDIDATE, 후보에 없는 ID를 지정하면 MANUAL로 결정 근거가 기록됩니다.
			존재하지 않는 ID를 지정하면 실패합니다.

			- CANDIDATE(후보 선택): 자동 매칭이 계산한 후보 목록에서 관리자가 선택한 경우에 해당 값이 사용된다
			- MANUAL(직접 선택): 자동매칭이 아닌 관리자가 직접 선택한 경우에 해당 값이 사용된다

			확정하면 이전에 정규화 배치가 남긴 판정 값은 위 두 값 중 하나로 대체됩니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "확정 처리 결과",
				content = [Content(schema = Schema(implementation = MfdsMatchingConfirmResponse::class))]
			),
			ApiResponse(
				responseCode = "400",
				description = "요청 본문이 올바르지 않거나, 선택한 주류·증류소·지역이 존재하지 않습니다.",
				content = [Content(schema = Schema(ref = ERROR_SCHEMA))]
			),
			ApiResponse(
				responseCode = "404",
				description = "요청한 수입 신고가 없습니다.",
				content = [Content(schema = Schema(ref = ERROR_SCHEMA))]
			)
		]
	)
	annotation class ConfirmMatching

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "매칭 확정을 해제한다",
		description = "확정된 위스키·증류소·지역 연결을 해제합니다. 저장된 후보와 매칭 이력(matchingVersion, matchedAt)은 유지됩니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "해제 처리 결과",
				content = [Content(schema = Schema(implementation = MfdsMatchingConfirmResponse::class))]
			),
			ApiResponse(
				responseCode = "404",
				description = "요청한 수입 신고가 없습니다.",
				content = [Content(schema = Schema(ref = ERROR_SCHEMA))]
			)
		]
	)
	annotation class ReleaseMatching

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "같은 제품 일괄 적용을 미리 본다",
		description = """
			기준 신고와 같은 product_identity_key_sha256 그룹을 부작용 없이 분류합니다.
			숙성, 배치, 캐스크, 연도, 숫자 에디션, 캐스크 스트렝스, 도수 유형, 도수를 추가로 비교합니다.
			용량, 수입사, 통관일 차이는 같은 제품으로 봅니다. 식별 정보가 한쪽에만 있으면 확인 필요이며 자동으로 같은 제품이라고 보지 않습니다.
			이미 다른 주류·증류소·지역이 있으면 충돌이며 덮어쓰지 않습니다. 같은 3종이면 변경 불필요합니다.
			분류 우선순위는 충돌, 변경 불필요, 확인 필요, 적용 가능입니다. 확인 필요 사유는 여러 개일 수 있습니다.
			버전 표기, 변이 표기, 정제 검토 필요, 일반명, 제조국 불일치, 관리자 해제 이력도 확인 필요합니다.
			증류소와 지역을 생략하면 주류에 등록된 값을 사용합니다.
			previewToken은 64자 난수 발급 식별자이고 기존 Redis에 10분간 저장합니다. 상태 해시는 그 토큰과 따로 다시 계산합니다.
			확정이 실패해 토큰이 소비되면 같은 토큰으로 다시 확정할 수 없고 미리보기를 다시 받아야 합니다.
			한 번에 다루는 신고는 500건까지이며 그 이상은 자르지 않고 거절합니다.
			이름 검색으로 찾은 다른 숙성 연수는 이 목록에 포함되지 않습니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "적용 가능, 변경 불필요, 확인 필요, 충돌 목록",
				content = [Content(schema = Schema(implementation = MfdsBulkMatchingPreviewResponse::class))]
			),
			ApiResponse(
				responseCode = "400",
				description = "요청이 올바르지 않거나, 제품 식별 키 또는 선택한 주류·증류소·지역이 없습니다.",
				content = [Content(schema = Schema(ref = ERROR_SCHEMA))]
			),
			ApiResponse(
				responseCode = "404",
				description = "기준 수입 신고가 없습니다.",
				content = [Content(schema = Schema(ref = ERROR_SCHEMA))]
			)
		]
	)
	annotation class PreviewBulkMatching

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "같은 제품 매칭을 일괄 확정한다",
		description = """
			미리보기에서 적용 가능하거나 변경이 불필요한 신고만 한 트랜잭션으로 확정합니다.
			적용할 주류·증류소·지역은 previewToken의 발급 기록에서 읽으므로 요청에는 previewToken과 고른 신고 ID만 보냅니다.
			응답은 반영된 신고의 확정 상태(applied)와 이미 같은 연결이라 바꾸지 않은 신고 ID(unchangedDeclarationIds)입니다.
			확인 필요, 충돌, 미리보기에 없던 ID, 중복 ID, 만료되었거나 데이터와 다른 previewToken이면 아무것도 반영하지 않습니다.
			같은 제품 그룹은 신고 ID 오름차순으로 잠급니다. 후보와 원문, 용량, 도수, 숙성, 배치 필드는 바꾸지 않습니다.
			각 신고의 감사 이력에 선택 근거와 기준 신고, 관리자를 남깁니다. 이후 수집분에 대한 자동 전파와 일괄 해제는 하지 않습니다.
			previewToken은 확정 요청에서 한 번 소비됩니다. 실패하거나 롤백되어도 그 토큰은 복구하지 않으므로 미리보기를 다시 받아야 합니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "일괄 확정 결과",
				content = [Content(schema = Schema(implementation = MfdsBulkMatchingConfirmResponse::class))]
			),
			ApiResponse(
				responseCode = "400",
				description = "요청이 올바르지 않거나 적용할 수 없는 신고가 포함되어 있습니다.",
				content = [Content(schema = Schema(ref = ERROR_SCHEMA))]
			),
			ApiResponse(
				responseCode = "404",
				description = "기준 수입 신고가 없습니다.",
				content = [Content(schema = Schema(ref = ERROR_SCHEMA))]
			),
			ApiResponse(
				responseCode = "409",
				description = "미리보기가 만료되었거나 그 사이 데이터가 바뀌었습니다.",
				content = [Content(schema = Schema(ref = ERROR_SCHEMA))]
			)
		]
	)
	annotation class ConfirmBulkMatching
}
