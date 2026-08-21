package app.bottlenote.mfds.presentation.docs

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

	@Target(AnnotationTarget.CLASS)
	@Retention(AnnotationRetention.RUNTIME)
	@Tag(name = "수입 원장 매칭", description = "정제된 수입 원장을 BottleNote 위스키·증류소·지역 데이터와 비교해 후보를 계산하고 확정한다")
	annotation class ApiTag

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "매칭을 실행한다",
		description = """
			신고 정제 데이터 한 건을 전체 위스키·증류소·지역과 비교해 유사도 점수를 계산합니다.

			점수 상위 3개 후보를 저장하고, 각 후보의 요소별 점수 근거(이름·도수·숙성·카테고리·지역)와 함께 반환합니다.
			다시 실행하면 기존 후보를 덮어씁니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "계산된 후보 목록과 점수 근거",
				content = [Content(schema = Schema(implementation = MfdsMatchingRunResponse::class))]
			)
		]
	)
	annotation class RunMatching

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "저장된 매칭 후보를 조회한다",
		description = "신고 정제 데이터에 저장된 후보 목록과 각 후보의 요약 정보, 현재 확정 상태를 조회합니다. 점수 근거 상세는 매칭 실행 응답에서만 제공됩니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "저장된 후보와 확정 상태",
				content = [Content(schema = Schema(implementation = MfdsMatchingCandidatesResponse::class))]
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

			후보 목록에 있는 ID를 고르면 CANDIDATE, 후보에 없는 ID를 지정하면 MANUAL(수동 매칭)로 결정 근거가 기록됩니다.
			존재하지 않는 ID를 지정하면 실패합니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "확정 처리 결과",
				content = [Content(schema = Schema(implementation = MfdsMatchingConfirmResponse::class))]
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
			)
		]
	)
	annotation class ReleaseMatching
}
