package app.bottlenote.statistics.presentation.docs

import app.bottlenote.alcohols.constant.PopularityAxis
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

object AdminAlcoholStatisticsApiDocs {
	@Target(AnnotationTarget.CLASS)
	@Retention(AnnotationRetention.RUNTIME)
	@Tag(name = "통계", description = "Admin 시계열 통계 API")
	annotation class ApiTag

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "주류 인기도 시계열을 조회한다",
		description = """
			alcohol_popularity_snapshots의 점수와 원시값을 표준 시계열로 내린다.
			지원 단위는 HOUR, WEEK, MONTH이며 DAY는 400이다. 구간 상한은 HOUR 31일, WEEK·MONTH 366일이다.
			배치는 닫힌 버킷만 만들므로 요청 구간의 마지막 주·월 버킷이 열려 있으면 그 버킷만 HOUR 행에서 롤다운한다.
			열린 버킷의 점수 5개는 전체 주류 정규화가 필요해 null이고 partial은 true다.
			""",
		parameters = [
			Parameter(
				name = "alcoholId",
				`in` = ParameterIn.PATH,
				required = true,
				schema = Schema(implementation = Long::class, description = "주류 ID")
			),
			Parameter(
				name = "from",
				`in` = ParameterIn.QUERY,
				required = false,
				schema = Schema(implementation = LocalDate::class, description = "조회 시작일", example = "2026-08-01")
			),
			Parameter(
				name = "to",
				`in` = ParameterIn.QUERY,
				required = false,
				schema = Schema(implementation = LocalDate::class, description = "조회 종료일", example = "2026-09-07")
			),
			Parameter(
				name = "granularity",
				`in` = ParameterIn.QUERY,
				required = false,
				schema = Schema(
					implementation = TimeSeriesGranularity::class,
					description = "집계 단위. HOUR, WEEK, MONTH만 지원하며 기본값은 WEEK다",
					defaultValue = "WEEK",
					allowableValues = ["HOUR", "WEEK", "MONTH"]
				)
			)
		]
	)
	@ApiResponse(
		responseCode = "200",
		description = "인기도 시계열",
		content = [Content(schema = Schema(implementation = AlcoholPopularityTimeSeriesEnvelope::class))]
	)
	annotation class GetPopularity

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "주류 축별 관측 시계열을 조회한다",
		description = """
			축별 관측 테이블의 흐름값·상태값과 유도값을 표준 시계열로 내린다.
			지원 단위는 HOUR, WEEK, MONTH이며 DAY는 400이다. 구간 상한은 HOUR 31일, WEEK·MONTH 366일이다.
			열린 주·월 버킷은 HOUR 행에서 롤다운하고 partial은 true다. 흐름값은 합, 상태값은 기간 안 최신 HOUR 값이다.
			""",
		parameters = [
			Parameter(
				name = "alcoholId",
				`in` = ParameterIn.PATH,
				required = true,
				schema = Schema(implementation = Long::class, description = "주류 ID")
			),
			Parameter(
				name = "axis",
				`in` = ParameterIn.PATH,
				required = true,
				schema = Schema(implementation = PopularityAxis::class, description = "관측 축")
			),
			Parameter(
				name = "from",
				`in` = ParameterIn.QUERY,
				required = false,
				schema = Schema(implementation = LocalDate::class, description = "조회 시작일", example = "2026-08-01")
			),
			Parameter(
				name = "to",
				`in` = ParameterIn.QUERY,
				required = false,
				schema = Schema(implementation = LocalDate::class, description = "조회 종료일", example = "2026-09-07")
			),
			Parameter(
				name = "granularity",
				`in` = ParameterIn.QUERY,
				required = false,
				schema = Schema(
					implementation = TimeSeriesGranularity::class,
					description = "집계 단위. HOUR, WEEK, MONTH만 지원하며 기본값은 WEEK다",
					defaultValue = "WEEK",
					allowableValues = ["HOUR", "WEEK", "MONTH"]
				)
			)
		]
	)
	@ApiResponse(
		responseCode = "200",
		description = "축별 관측 시계열",
		content = [Content(schema = Schema(implementation = AlcoholPopularityTimeSeriesEnvelope::class))]
	)
	annotation class GetObservations

	@Schema(name = "AlcoholPopularityTimeSeriesEnvelope")
	data class AlcoholPopularityTimeSeriesEnvelope(
		val success: Boolean,
		val code: Int,
		val data: TimeSeries,
		val errors: List<Any> = emptyList(),
		val meta: Map<String, Any?> = emptyMap()
	)
}
