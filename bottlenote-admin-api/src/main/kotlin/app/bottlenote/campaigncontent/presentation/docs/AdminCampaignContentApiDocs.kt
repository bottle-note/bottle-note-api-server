package app.bottlenote.campaigncontent.presentation.docs

import app.bottlenote.campaigncontent.dto.response.AdminCampaignContentDetailResponse
import app.bottlenote.campaigncontent.dto.response.AdminCampaignContentListResponse
import app.bottlenote.campaigncontent.dto.response.AdminCampaignContentMetricsResponse
import app.bottlenote.global.dto.response.AdminResultResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

/** 캠페인 콘텐츠 엔드포인트의 문서 설명. */
object AdminCampaignContentApiDocs {

	@Target(AnnotationTarget.CLASS)
	@Retention(AnnotationRetention.RUNTIME)
	@Tag(name = "캠페인 콘텐츠")
	annotation class ApiTag

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "캠페인 콘텐츠 목록을 조회한다",
		description = """
			이름·코드 검색어, 활성 여부, 페이지 번호·크기로 캠페인 콘텐츠를 최신 등록순으로 조회합니다.

			recentParticipants는 오늘을 포함한 최근 7일간 결과를 조회한 순회원 수이며 방문자 통계 제외 규칙을 적용합니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "캠페인 콘텐츠 목록",
				content = [
					Content(
						array = ArraySchema(schema = Schema(implementation = AdminCampaignContentListResponse::class))
					)
				]
			)
		]
	)
	annotation class GetCampaignContents

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "캠페인 콘텐츠 상세 정보를 조회한다",
		description = "캠페인 콘텐츠 ID로 단일 캠페인 콘텐츠를 조회합니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "캠페인 콘텐츠 상세 정보",
				content = [Content(schema = Schema(implementation = AdminCampaignContentDetailResponse::class))]
			)
		]
	)
	annotation class GetCampaignContentDetail

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "캠페인 콘텐츠를 등록한다",
		description = """
			FE 배포 전에 캠페인 콘텐츠의 코드를 등록합니다.

			코드는 영문 소문자·숫자와 단어 사이 하이픈만 쓸 수 있고 50자 이하여야 하며, 이미 쓰는 코드와 겹치면 실패합니다.
			등록한 코드는 수정할 수 없습니다. isActive를 비우면 활성 상태로 등록합니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "등록 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			)
		]
	)
	annotation class CreateCampaignContent

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "캠페인 콘텐츠 정보를 수정한다",
		description = "이름, 설명, 활성 여부를 수정합니다. 코드는 FE 배포물에 들어가 있어 수정 대상이 아닙니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "수정 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			)
		]
	)
	annotation class UpdateCampaignContent

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "캠페인 콘텐츠를 삭제한다",
		description = "참여 기록이 없는 캠페인 콘텐츠만 삭제합니다. 참여 기록이 있으면 실패하므로 비활성화해 주세요.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "삭제 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			)
		]
	)
	annotation class DeleteCampaignContent

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "캠페인 콘텐츠 활성 상태를 변경한다",
		description = "비활성 캠페인 콘텐츠의 코드로 들어온 참여 이벤트는 미등록 코드와 같이 거절됩니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "상태 변경 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			)
		]
	)
	annotation class UpdateCampaignContentStatus

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "캠페인 콘텐츠 참여 지표를 조회한다",
		description = """
			from·to(양 끝 포함, Asia/Seoul) 기간의 참여 지표를 조회합니다. 비우면 to는 오늘, from은 to를 포함한 최근 7일입니다.

			- 진입·시작·완료는 순방문자, 결과 조회는 순회원 기준입니다.
			- 완주율은 시작한 방문자 중 완료한 방문자, 로그인 전환율은 완료한 방문자 중 결과까지 조회한 방문자의 비율입니다.
			- 참여율은 같은 기간 서비스 전체 활성 회원 중 결과를 조회한 회원의 비율입니다.
			- 방문자 통계와 같은 제외 규칙(봇·도구, 제외 IP 대역, IP 없음, 루트 관리자)을 적용합니다.
			- 방문 로그는 90일만 보관하므로 오늘 기준 90일보다 이전이거나 미래가 포함된 기간은 실패합니다.
			- 오늘 값은 방문 로그 적재 지연으로 잠정치입니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "참여 지표",
				content = [Content(schema = Schema(implementation = AdminCampaignContentMetricsResponse::class))]
			)
		]
	)
	annotation class GetCampaignContentMetrics
}
