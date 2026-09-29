package app.bottlenote.statistics.presentation.docs

import app.bottlenote.global.timeseries.TimeSeriesGranularity
import app.bottlenote.statistics.dto.response.MemberCohortRetentionResponse
import app.bottlenote.statistics.dto.response.MemberFunnelResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import java.time.LocalDate

/** 회원 전환·리텐션 통계 엔드포인트의 문서 설명. */
object AdminMemberStatisticsApiDocs {

	@Target(AnnotationTarget.CLASS)
	@Retention(AnnotationRetention.RUNTIME)
	@Tag(name = "통계")
	annotation class ApiTag

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "회원 전환 퍼널을 조회한다",
		description = """
			방문(VISITED) → 가입(SIGNED_UP) → 필수 약관 동의(AGREED) → 첫 행동(ACTIVATED) 단계별 인원을 내립니다.

			방문은 구간 안 고유 방문자 쿠키 수이며 방문자 통계와 같은 제외 규칙을 적용합니다.
			가입은 users.create_at이 구간 안인 회원 수입니다. 약관 동의와 첫 행동은 그 가입 회원의 조회 시점 현재 상태입니다.
			첫 행동은 리뷰, 별점(0 초과), 찜 중 하나라도 남긴 경우입니다. 루트 관리자는 모든 단계에서 제외합니다.

			conversionRate는 직전 단계 대비, overallRate는 방문 대비 비율(%)입니다.
			구간 상한은 90일이며 from과 to를 생략하면 오늘 기준 최근 30일입니다.
			""",
		parameters = [
			Parameter(
				name = "from",
				`in` = ParameterIn.QUERY,
				description = "조회 시작일 (ISO 날짜). 생략하면 to-29일",
				schema = Schema(implementation = LocalDate::class)
			),
			Parameter(
				name = "to",
				`in` = ParameterIn.QUERY,
				description = "조회 종료일 (ISO 날짜). 생략하면 오늘(Asia/Seoul)",
				schema = Schema(implementation = LocalDate::class)
			)
		],
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "회원 전환 퍼널",
				content = [Content(schema = Schema(implementation = MemberFunnelResponse::class))]
			)
		]
	)
	annotation class Funnel

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "가입 코호트 리텐션을 조회한다",
		description = """
			from~to에 가입한 회원을 granularity 버킷으로 묶고, 각 코호트가 가입 버킷으로부터 offset번째 버킷에 다시 활동한 비율을 내립니다.
			offset 0은 가입 버킷 자신입니다.

			활동 원천은 배치가 매일 쌓는 user_daily_activities 롤업입니다. coverageFrom보다 앞선 날짜의 활동은 관측되지 않은 것이므로
			그 이전 코호트의 값은 실제보다 낮게 보입니다. 아직 시작되지 않은 버킷은 activeMembers와 retentionRate가 null이고,
			진행 중인 버킷은 partial=true입니다. 루트 관리자는 제외합니다.

			구간 상한은 366일이며 from과 to를 생략하면 오늘 기준 최근 30일입니다.
			offsets 기본값은 DAY 14, WEEK 8, MONTH 6이고 최대 24입니다.
			""",
		parameters = [
			Parameter(
				name = "from",
				`in` = ParameterIn.QUERY,
				description = "가입일 구간 시작 (ISO 날짜). 생략하면 to-29일",
				schema = Schema(implementation = LocalDate::class)
			),
			Parameter(
				name = "to",
				`in` = ParameterIn.QUERY,
				description = "가입일 구간 종료 (ISO 날짜). 생략하면 오늘(Asia/Seoul)",
				schema = Schema(implementation = LocalDate::class)
			),
			Parameter(
				name = "granularity",
				`in` = ParameterIn.QUERY,
				description = "코호트 단위. DAY, WEEK, MONTH만 지원하며 기본값은 WEEK",
				schema = Schema(implementation = TimeSeriesGranularity::class)
			),
			Parameter(
				name = "offsets",
				`in` = ParameterIn.QUERY,
				description = "코호트마다 내릴 버킷 수 (1~24). 생략하면 단위별 기본값",
				schema = Schema(implementation = Int::class)
			)
		],
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "가입 코호트 리텐션 행렬",
				content = [Content(schema = Schema(implementation = MemberCohortRetentionResponse::class))]
			)
		]
	)
	annotation class CohortRetention
}
