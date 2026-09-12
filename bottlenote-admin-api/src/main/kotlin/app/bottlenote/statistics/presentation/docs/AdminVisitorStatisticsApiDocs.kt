package app.bottlenote.statistics.presentation.docs

import app.bottlenote.global.timeseries.TimeSeries
import app.bottlenote.global.timeseries.TimeSeriesGranularity
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import java.time.LocalDate

/** 방문자 통계 시계열 엔드포인트의 문서 설명. */
object AdminVisitorStatisticsApiDocs {

	@Target(AnnotationTarget.CLASS)
	@Retention(AnnotationRetention.RUNTIME)
	@Tag(name = "통계")
	annotation class ApiTag

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "방문자 활동 시계열을 조회한다",
		description = """
			고유 방문자 수(visitors)와 고유 회원 수(members)를 DAY, WEEK, MONTH 단위로 조회합니다.

			구간 상한은 90일입니다. 마지막 버킷은 요청 시점까지 집계된 부분 값이며 partial=true입니다.
			from과 to를 생략하면 오늘 기준 최근 30일입니다.
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
			),
			Parameter(
				name = "granularity",
				`in` = ParameterIn.QUERY,
				description = "집계 단위. DAY, WEEK, MONTH만 지원하며 기본값은 DAY",
				schema = Schema(implementation = TimeSeriesGranularity::class)
			)
		],
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "방문자 활동 시계열",
				content = [Content(schema = Schema(implementation = TimeSeries::class))]
			)
		]
	)
	annotation class Active

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "방문자 재방문 시계열을 조회한다",
		description = """
			재방문율은 해당 버킷 방문자 중 직전 버킷에도 방문한 비율입니다. 첫 버킷의 직전 버킷은 요청 구간 밖이어도 읽습니다.

			구간 상한은 90일입니다. 마지막 버킷은 요청 시점까지 집계된 부분 값이며 partial=true입니다.
			from과 to를 생략하면 오늘 기준 최근 30일입니다.
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
			),
			Parameter(
				name = "granularity",
				`in` = ParameterIn.QUERY,
				description = "집계 단위. DAY, WEEK, MONTH만 지원하며 기본값은 DAY",
				schema = Schema(implementation = TimeSeriesGranularity::class)
			)
		],
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "방문자 재방문 시계열",
				content = [Content(schema = Schema(implementation = TimeSeries::class))]
			)
		]
	)
	annotation class FindRetention
}
