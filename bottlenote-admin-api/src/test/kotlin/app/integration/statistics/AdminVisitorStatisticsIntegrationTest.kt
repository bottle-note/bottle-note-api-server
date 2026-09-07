package app.integration.statistics

import app.IntegrationTestSupport
import app.bottlenote.statistics.fixture.VisitorTelemetryTestFactory
import app.bottlenote.user.fixture.UserTestFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

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

		val member = userTestFactory.persistUser()
		val rootAdminUser = userTestFactory.persistRootAdminUser()

		val todayNoon = today.atTime(LocalTime.NOON)
		val yesterdayNoon = yesterday.atTime(LocalTime.NOON)

		visitorTelemetryTestFactory.persistEvent(yesterdayNoon, visitor("A"), null, "203.0.113.10", "모바일")
		visitorTelemetryTestFactory.persistEvent(todayNoon, visitor("A"), null, "203.0.113.10", "모바일")
		visitorTelemetryTestFactory.persistEvent(todayNoon, visitor("B"), member.id, "203.0.113.11", "모바일")
		visitorTelemetryTestFactory.persistEvent(todayNoon, visitor("BOT"), null, "203.0.113.12", "봇")
		visitorTelemetryTestFactory.persistEvent(todayNoon, visitor("IP"), null, "66.249.1.1", "모바일")
		visitorTelemetryTestFactory.persistEvent(todayNoon, visitor("NULL"), null, null, "모바일")
		visitorTelemetryTestFactory.persistEvent(todayNoon, visitor("ROOT"), rootAdminUser.id, "203.0.113.13", "모바일")
	}

	@Test
	@DisplayName("오늘 활동 시계열에서 제외 규칙을 적용한 방문자와 회원 수를 반환한다")
	fun getTodayActiveSeries() {
		val result = mockMvcTester
			.get()
			.uri("/v1/statistics/visitors/active")
			.param("from", today.toString())
			.param("to", today.toString())
			.param("granularity", "DAY")
			.header("Authorization", "Bearer $accessToken")
			.exchange()

		assertThat(result).hasStatusOk()
		assertThat(result).bodyJson().extractingPath("$.data.points.length()").isEqualTo(1)
		assertThat(result).bodyJson().extractingPath("$.data.points[0].values.visitors").isEqualTo(2)
		assertThat(result).bodyJson().extractingPath("$.data.points[0].values.members").isEqualTo(1)
		assertThat(result).bodyJson().extractingPath("$.data.points[0].partial").isEqualTo(true)
	}

	@Test
	@DisplayName("오늘 재방문 시계열에서 재방문자는 1명, 재방문율은 50.0이다")
	fun getTodayRetentionSeries() {
		val result = mockMvcTester
			.get()
			.uri("/v1/statistics/visitors/retention")
			.param("from", today.toString())
			.param("to", today.toString())
			.param("granularity", "DAY")
			.header("Authorization", "Bearer $accessToken")
			.exchange()

		assertThat(result).hasStatusOk()
		assertThat(result).bodyJson().extractingPath("$.data.points.length()").isEqualTo(1)
		assertThat(result).bodyJson().extractingPath("$.data.points[0].values.visitors").isEqualTo(2)
		assertThat(result).bodyJson().extractingPath("$.data.points[0].values.returningVisitors").isEqualTo(1)
		assertThat(result).bodyJson().extractingPath("$.data.points[0].values.retentionRate").isEqualTo(50.0)
		assertThat(result).bodyJson().extractingPath("$.data.points[0].partial").isEqualTo(true)
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

	private fun visitor(label: String): String = label.padEnd(64, '0')
}
