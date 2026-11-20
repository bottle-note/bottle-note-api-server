package app.integration.statistics

import app.IntegrationTestSupport
import app.bottlenote.global.timeseries.TimeSeriesGranularity
import app.bottlenote.statistics.fixture.VisitorTelemetryTestFactory
import app.bottlenote.user.fixture.UserTestFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.assertj.MvcTestResult
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

@Tag("admin_integration")
@DisplayName("[integration] Admin 방문자 통계 API 통합 테스트")
class AdminVisitorStatisticsIntegrationTest : IntegrationTestSupport() {

	@Autowired
	private lateinit var visitorTelemetryTestFactory: VisitorTelemetryTestFactory

	@Autowired
	private lateinit var userTestFactory: UserTestFactory

	private lateinit var accessToken: String
	private val zone: ZoneId = ZoneId.of("Asia/Seoul")
	private lateinit var today: LocalDate
	private lateinit var yesterday: LocalDate

	@BeforeEach
	fun setUp() {
		val admin = adminUserTestFactory.persistRootAdmin()
		accessToken = getAccessToken(admin)
		today = LocalDate.now(zone)
		yesterday = today.minusDays(1)
	}

	@Test
	@DisplayName("오늘 활동 시계열에서 방문자와 회원 수를 반환한다")
	fun getTodayActiveSeries() {
		val member = userTestFactory.persistUser()
		persist("A", yesterday.atNoon(), ip = "203.0.113.10")
		persist("A", today.atNoon(), ip = "203.0.113.10")
		persist("B", today.atNoon(), userId = member.id, ip = "203.0.113.11")

		val result = get("/active", today, today, "DAY")

		assertThat(result).hasStatusOk()
		assertThat(result).bodyJson().extractingPath("$.data.points.length()").isEqualTo(1)
		assertThat(result).bodyJson().extractingPath("$.data.points[0].values.visitors").isEqualTo(2)
		assertThat(result).bodyJson().extractingPath("$.data.points[0].values.members").isEqualTo(1)
		assertThat(result).bodyJson().extractingPath("$.data.points[0].partial").isEqualTo(true)
	}

	@Test
	@DisplayName("오늘 재방문 시계열에서 재방문자는 1명, 재방문율은 50.0이다")
	fun getTodayRetentionSeries() {
		val member = userTestFactory.persistUser()
		persist("A", yesterday.atNoon(), ip = "203.0.113.10")
		persist("A", today.atNoon(), ip = "203.0.113.10")
		persist("B", today.atNoon(), userId = member.id, ip = "203.0.113.11")

		val result = get("/retention", today, today, "DAY")

		assertThat(result).hasStatusOk()
		assertThat(result).bodyJson().extractingPath("$.data.points.length()").isEqualTo(1)
		assertThat(result).bodyJson().extractingPath("$.data.points[0].values.visitors").isEqualTo(2)
		assertThat(result).bodyJson().extractingPath("$.data.points[0].values.returningVisitors").isEqualTo(1)
		assertThat(result).bodyJson().extractingPath("$.data.points[0].values.retentionRate").isEqualTo(50.0)
		assertThat(result).bodyJson().extractingPath("$.data.points[0].partial").isEqualTo(true)
	}

	@ParameterizedTest
	@EnumSource(TimeSeriesGranularity::class, names = ["DAY", "WEEK", "MONTH"])
	@DisplayName("현재와 직전 구간에 반복 방문할 때 방문자와 재방문자를 각각 한 번만 센다")
	fun countRepeatedVisitsOnce(granularity: TimeSeriesGranularity) {
		val current = granularity.previous(granularity.truncate(today.atStartOfDay()))
		val previous = granularity.previous(current)
		val older = granularity.previous(previous)
		val member = userTestFactory.persistUser()
		repeat(3) { offset ->
			persist("RETURNING", previous.plusHours(offset.toLong()), ip = "203.0.113.10")
			persist("PREVIOUS_ONLY", previous.plusHours(offset.toLong()), ip = "203.0.113.11")
		}
		repeat(4) { offset ->
			persist("RETURNING", current.plusHours(offset.toLong()), userId = member.id, ip = "203.0.113.10")
			persist("NEW", current.plusHours(offset.toLong()), ip = "203.0.113.12")
			persist("GAP", current.plusHours(offset.toLong()), ip = "203.0.113.13")
		}
		persist("GAP", older.plusHours(1), ip = "203.0.113.13")

		val result = get("/retention", current.toLocalDate(), current.toLocalDate(), granularity.name)

		assertThat(result).hasStatusOk()
		assertThat(result).bodyJson().extractingPath("$.data.points.length()").isEqualTo(1)
		assertThat(result).bodyJson().extractingPath("$.data.points[0].values.visitors").isEqualTo(3)
		assertThat(result).bodyJson().extractingPath("$.data.points[0].values.returningVisitors").isEqualTo(1)
		assertThat(result).bodyJson().extractingPath("$.data.points[0].values.retentionRate").isEqualTo(33.3)
		assertThat(result).bodyJson().extractingPath("$.data.points[0].partial").isEqualTo(false)
	}

	@Test
	@DisplayName("방문 기록이 없는 날이 끼어 있을 때 7일 포인트를 유지하고 재방문으로 세지 않는다")
	fun preserveEmptyDaysWithoutCountingNonConsecutiveVisits() {
		val from = today.minusDays(7)
		val to = yesterday
		persist("GAP", from.atNoon(), ip = "203.0.113.10")
		persist("GAP", from.plusDays(2).atNoon(), ip = "203.0.113.10")

		val result = get("/retention", from, to, "DAY")

		assertThat(result).hasStatusOk()
		assertThat(result).bodyJson().extractingPath("$.data.points.length()").isEqualTo(7)
		for (index in 0..6) {
			val point = "$.data.points[$index]"
			assertThat(result).bodyJson().extractingPath("$point.bucketAt")
				.isEqualTo("${from.plusDays(index.toLong())}T00:00:00")
			assertThat(result).bodyJson().extractingPath("$point.values.visitors")
				.isEqualTo(if (index == 0 || index == 2) 1 else 0)
			assertThat(result).bodyJson().extractingPath("$point.values.returningVisitors").isEqualTo(0)
			assertThat(result).bodyJson().extractingPath("$point.values.retentionRate")
				.isIn(0, 0.0)
			assertThat(result).bodyJson().extractingPath("$point.partial").isEqualTo(false)
		}
	}

	@Test
	@DisplayName("봇 device_type 행은 방문자 수에서 제외된다")
	fun excludeBotDeviceType() {
		persist("OK", today.atNoon(), ip = "203.0.113.10")
		persist("BOT", today.atNoon(), ip = "203.0.113.12", deviceType = "봇")

		val result = get("/active", today, today, "DAY")

		assertThat(result).hasStatusOk()
		assertThat(result).bodyJson().extractingPath("$.data.points[0].values.visitors").isEqualTo(1)
	}

	@Test
	@DisplayName("제외 IP prefix 행은 방문자 수에서 제외된다")
	fun excludeIpPrefix() {
		persist("OK", today.atNoon(), ip = "203.0.113.10")
		persist("IP", today.atNoon(), ip = "66.249.1.1")

		val result = get("/active", today, today, "DAY")

		assertThat(result).hasStatusOk()
		assertThat(result).bodyJson().extractingPath("$.data.points[0].values.visitors").isEqualTo(1)
	}

	@Test
	@DisplayName("NULL IP 행은 방문자 수에서 제외된다")
	fun excludeNullIp() {
		persist("OK", today.atNoon(), ip = "203.0.113.10")
		persist("NULL", today.atNoon(), ip = null)

		val result = get("/active", today, today, "DAY")

		assertThat(result).hasStatusOk()
		assertThat(result).bodyJson().extractingPath("$.data.points[0].values.visitors").isEqualTo(1)
	}

	@Test
	@DisplayName("root_admins user_id 행은 방문자 수에서 제외된다")
	fun excludeRootAdminUser() {
		val rootAdminUser = userTestFactory.persistUserAsRootAdmin()
		persist("OK", today.atNoon(), ip = "203.0.113.10")
		persist("ROOT", today.atNoon(), userId = rootAdminUser.id, ip = "203.0.113.13")

		val result = get("/active", today, today, "DAY")

		assertThat(result).hasStatusOk()
		assertThat(result).bodyJson().extractingPath("$.data.points[0].values.visitors").isEqualTo(1)
	}

	@Test
	@DisplayName("주 경계를 넘는 재방문 시계열은 지난주와 이번 주 버킷을 구분한다")
	fun retainAcrossWeekBoundary() {
		val thisMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
		val lastSunday = thisMonday.minusDays(1)
		val lastMonday = thisMonday.minusWeeks(1)
		persist("W", lastSunday.atNoon(), ip = "203.0.113.10")
		persist("W", thisMonday.atNoon(), ip = "203.0.113.10")

		val result = get("/retention", lastMonday, today, "WEEK")

		assertThat(result).hasStatusOk()
		assertThat(result).bodyJson().extractingPath("$.data.points.length()").isEqualTo(2)
		assertThat(result).bodyJson().extractingPath("$.data.points[0].values.visitors").isEqualTo(1)
		assertThat(result).bodyJson().extractingPath("$.data.points[0].values.returningVisitors").isEqualTo(0)
		assertThat(result).bodyJson().extractingPath("$.data.points[0].partial").isEqualTo(false)
		assertThat(result).bodyJson().extractingPath("$.data.points[1].values.visitors").isEqualTo(1)
		assertThat(result).bodyJson().extractingPath("$.data.points[1].values.returningVisitors").isEqualTo(1)
		assertThat(result).bodyJson().extractingPath("$.data.points[1].partial").isEqualTo(true)
	}

	@Test
	@DisplayName("월 경계를 넘는 재방문 시계열은 지난달과 이번 달 버킷을 구분한다")
	fun retainAcrossMonthBoundary() {
		val thisMonthStart = today.withDayOfMonth(1)
		val lastMonthEnd = thisMonthStart.minusDays(1)
		val lastMonthStart = lastMonthEnd.withDayOfMonth(1)
		persist("M", lastMonthEnd.atNoon(), ip = "203.0.113.10")
		persist("M", thisMonthStart.atNoon(), ip = "203.0.113.10")

		val result = get("/retention", lastMonthStart, today, "MONTH")

		assertThat(result).hasStatusOk()
		assertThat(result).bodyJson().extractingPath("$.data.points.length()").isEqualTo(2)
		assertThat(result).bodyJson().extractingPath("$.data.points[0].values.visitors").isEqualTo(1)
		assertThat(result).bodyJson().extractingPath("$.data.points[0].values.returningVisitors").isEqualTo(0)
		assertThat(result).bodyJson().extractingPath("$.data.points[0].partial").isEqualTo(false)
		assertThat(result).bodyJson().extractingPath("$.data.points[1].values.visitors").isEqualTo(1)
		assertThat(result).bodyJson().extractingPath("$.data.points[1].values.returningVisitors").isEqualTo(1)
		assertThat(result).bodyJson().extractingPath("$.data.points[1].partial").isEqualTo(true)
	}

	@Test
	@DisplayName("직전 버킷의 봇 방문은 재방문자로 세지 않는다")
	fun excludeBotFromPreviousBucketRetention() {
		persist("R", yesterday.atNoon(), ip = "203.0.113.10", deviceType = "봇")
		persist("R", today.atNoon(), ip = "203.0.113.10")

		val result = get("/retention", today, today, "DAY")

		assertThat(result).hasStatusOk()
		assertThat(result).bodyJson().extractingPath("$.data.points[0].values.visitors").isEqualTo(1)
		assertThat(result).bodyJson().extractingPath("$.data.points[0].values.returningVisitors").isEqualTo(0)
	}

	@Test
	@DisplayName("정확히 90일 구간은 200을 반환한다")
	fun acceptExact90DayRange() {
		assertThat(
			mockMvcTester
				.get()
				.uri("/v1/statistics/visitors/active")
				.param("from", today.minusDays(89).toString())
				.param("to", today.toString())
				.param("granularity", "DAY")
				.header("Authorization", "Bearer $accessToken")
		).hasStatusOk()
	}

	@Test
	@DisplayName("to가 내일이면 400을 반환한다")
	fun rejectToAfterToday() {
		assertThat(
			mockMvcTester
				.get()
				.uri("/v1/statistics/visitors/active")
				.param("from", today.toString())
				.param("to", today.plusDays(1).toString())
				.param("granularity", "DAY")
				.header("Authorization", "Bearer $accessToken")
		).hasStatus(HttpStatus.BAD_REQUEST)
	}

	@Test
	@DisplayName("91일 구간을 요청하면 400을 반환한다")
	fun rejectRangeLongerThan90Days() {
		assertThat(
			mockMvcTester
				.get()
				.uri("/v1/statistics/visitors/active")
				.param("from", today.minusDays(90).toString())
				.param("to", today.toString())
				.param("granularity", "DAY")
				.header("Authorization", "Bearer $accessToken")
		).hasStatus(HttpStatus.BAD_REQUEST)
	}

	@Test
	@DisplayName("HOUR 단위를 요청하면 400을 반환한다")
	fun rejectHourGranularity() {
		assertThat(
			mockMvcTester
				.get()
				.uri("/v1/statistics/visitors/active")
				.param("from", today.toString())
				.param("to", today.toString())
				.param("granularity", "HOUR")
				.header("Authorization", "Bearer $accessToken")
		).hasStatus(HttpStatus.BAD_REQUEST)
	}

	@Test
	@DisplayName("알 수 없는 granularity 문자열의 응답 코드를 확인한다")
	fun rejectUnknownGranularity() {
		val result = mockMvcTester
			.get()
			.uri("/v1/statistics/visitors/active")
			.param("from", today.toString())
			.param("to", today.toString())
			.param("granularity", "XYZ")
			.header("Authorization", "Bearer $accessToken")
			.exchange()

		assertThat(result.response.status).isEqualTo(400)
	}

	@Test
	@DisplayName("토큰 없이 요청하면 403을 반환한다")
	fun rejectUnauthenticatedRequest() {
		assertThat(
			mockMvcTester
				.get()
				.uri("/v1/statistics/visitors/active")
				.param("from", today.toString())
				.param("to", today.toString())
		).hasStatus(HttpStatus.FORBIDDEN)
	}

	private fun get(
		path: String,
		from: LocalDate,
		to: LocalDate,
		granularity: String
	): MvcTestResult = mockMvcTester
		.get()
		.uri("/v1/statistics/visitors$path")
		.param("from", from.toString())
		.param("to", to.toString())
		.param("granularity", granularity)
		.header("Authorization", "Bearer $accessToken")
		.exchange()

	private fun persist(
		label: String,
		occurredAt: LocalDateTime,
		userId: Long? = null,
		ip: String?,
		deviceType: String = "모바일"
	) {
		visitorTelemetryTestFactory.persistEvent(occurredAt, visitor(label), userId, ip, deviceType)
	}

	private fun LocalDate.atNoon(): LocalDateTime = atTime(LocalTime.NOON)

	private fun visitor(label: String): String = label.padEnd(64, '0')
}
