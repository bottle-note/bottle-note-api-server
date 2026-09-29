package app.integration.statistics

import app.IntegrationTestSupport
import app.bottlenote.agreement.constant.AgreementAction
import app.bottlenote.agreement.constant.AgreementType
import app.bottlenote.picks.fixture.PicksTestFactory
import app.bottlenote.statistics.fixture.MemberStatisticsTestFactory
import app.bottlenote.statistics.fixture.VisitorTelemetryTestFactory
import app.bottlenote.user.fixture.UserTestFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.assertj.MvcTestResult
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

@Tag("admin_integration")
@DisplayName("[integration] Admin 회원 통계 API 통합 테스트")
class AdminMemberStatisticsIntegrationTest : IntegrationTestSupport() {

	@Autowired
	private lateinit var visitorTelemetryTestFactory: VisitorTelemetryTestFactory

	@Autowired
	private lateinit var memberStatisticsTestFactory: MemberStatisticsTestFactory

	@Autowired
	private lateinit var userTestFactory: UserTestFactory

	@Autowired
	private lateinit var picksTestFactory: PicksTestFactory

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
	@DisplayName("전환 퍼널은 방문·가입·필수 약관 동의·첫 행동 순으로 인원과 전환율을 내린다")
	fun getFunnel() {
		val activated = userTestFactory.persistUser()
		val agreedOnly = userTestFactory.persistUser()
		val neither = userTestFactory.persistUser()
		val rootAdminUser = userTestFactory.persistUserAsRootAdmin()
		listOf(activated, agreedOnly, neither, rootAdminUser).forEach {
			memberStatisticsTestFactory.updateSignedUpAt(it.id, yesterday.atTime(10, 0))
		}
		memberStatisticsTestFactory.agreeRequired(activated.id, yesterday.atTime(10, 1))
		memberStatisticsTestFactory.agreeRequired(agreedOnly.id, yesterday.atTime(10, 1))
		picksTestFactory.persistPicks(1L, activated.id)
		persistVisit("V1", yesterday.atTime(9, 0), ip = "203.0.113.10")
		persistVisit("V1", today.atTime(9, 0), ip = "203.0.113.10")
		persistVisit("V2", today.atTime(9, 0), userId = activated.id, ip = "203.0.113.11")
		persistVisit("BOT", today.atTime(9, 0), ip = "203.0.113.12", deviceType = "봇")

		val result = funnel(yesterday, today)

		assertThat(result).hasStatusOk()
		assertThat(result).bodyJson().extractingPath("$.data.stages.length()").isEqualTo(4)
		assertThat(result).bodyJson().extractingPath("$.data.stages[0].stage").isEqualTo("VISITED")
		assertThat(result).bodyJson().extractingPath("$.data.stages[0].count").isEqualTo(2)
		assertThat(result).bodyJson().extractingPath("$.data.stages[0].overallRate").isEqualTo(100.0)
		assertThat(result).bodyJson().extractingPath("$.data.stages[1].stage").isEqualTo("SIGNED_UP")
		assertThat(result).bodyJson().extractingPath("$.data.stages[1].count").isEqualTo(3)
		assertThat(result).bodyJson().extractingPath("$.data.stages[1].conversionRate").isEqualTo(150.0)
		assertThat(result).bodyJson().extractingPath("$.data.stages[2].stage").isEqualTo("AGREED")
		assertThat(result).bodyJson().extractingPath("$.data.stages[2].count").isEqualTo(2)
		assertThat(result).bodyJson().extractingPath("$.data.stages[2].conversionRate").isEqualTo(66.7)
		assertThat(result).bodyJson().extractingPath("$.data.stages[3].stage").isEqualTo("ACTIVATED")
		assertThat(result).bodyJson().extractingPath("$.data.stages[3].count").isEqualTo(1)
		assertThat(result).bodyJson().extractingPath("$.data.stages[3].conversionRate").isEqualTo(50.0)
		assertThat(result).bodyJson().extractingPath("$.data.stages[3].overallRate").isEqualTo(50.0)
	}

	@Test
	@DisplayName("필수 약관을 철회한 회원은 동의 단계에서 빠진다")
	fun revokedAgreementIsNotCounted() {
		val revoked = userTestFactory.persistUser()
		memberStatisticsTestFactory.updateSignedUpAt(revoked.id, yesterday.atTime(10, 0))
		memberStatisticsTestFactory.agreeRequired(revoked.id, yesterday.atTime(10, 1))
		memberStatisticsTestFactory.persistAgreement(
			revoked.id,
			AgreementType.TERMS_OF_SERVICE,
			AgreementAction.REVOKE,
			yesterday.atTime(11, 0)
		)

		val result = funnel(yesterday, today)

		assertThat(result).hasStatusOk()
		assertThat(result).bodyJson().extractingPath("$.data.stages[1].count").isEqualTo(1)
		assertThat(result).bodyJson().extractingPath("$.data.stages[2].count").isEqualTo(0)
	}

	@Test
	@DisplayName("구간 밖에 가입한 회원은 퍼널에 세지 않는다")
	fun signedUpOutsideRangeIsExcluded() {
		val old = userTestFactory.persistUser()
		memberStatisticsTestFactory.updateSignedUpAt(old.id, today.minusDays(10).atTime(10, 0))

		val result = funnel(yesterday, today)

		assertThat(result).hasStatusOk()
		assertThat(result).bodyJson().extractingPath("$.data.stages[1].count").isEqualTo(0)
	}

	@Test
	@DisplayName("주 단위 코호트 리텐션은 가입 주와 그 다음 주의 활동 회원 비율을 내린다")
	fun getWeeklyCohortRetention() {
		val thisMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
		val lastMonday = thisMonday.minusWeeks(1)
		val returning = userTestFactory.persistUser()
		val churned = userTestFactory.persistUser()
		val rootAdminUser = userTestFactory.persistUserAsRootAdmin()
		listOf(returning, churned, rootAdminUser).forEach {
			memberStatisticsTestFactory.updateSignedUpAt(it.id, lastMonday.atTime(10, 0))
		}
		memberStatisticsTestFactory.persistDailyActivity(returning.id, lastMonday)
		memberStatisticsTestFactory.persistDailyActivity(returning.id, lastMonday.plusDays(3))
		memberStatisticsTestFactory.persistDailyActivity(returning.id, thisMonday)
		memberStatisticsTestFactory.persistDailyActivity(churned.id, lastMonday)
		memberStatisticsTestFactory.persistDailyActivity(rootAdminUser.id, thisMonday)

		val result = cohortRetention(lastMonday, today, "WEEK", 3)

		assertThat(result).hasStatusOk()
		assertThat(result).bodyJson().extractingPath("$.data.granularity").isEqualTo("WEEK")
		assertThat(result).bodyJson().extractingPath("$.data.offsets").isEqualTo(3)
		assertThat(result).bodyJson().extractingPath("$.data.coverageFrom").isEqualTo(lastMonday.toString())
		assertThat(result).bodyJson().extractingPath("$.data.cohorts.length()").isEqualTo(2)
		val cohort = "$.data.cohorts[0]"
		assertThat(result).bodyJson().extractingPath("$cohort.cohortAt").isEqualTo("${lastMonday}T00:00:00")
		assertThat(result).bodyJson().extractingPath("$cohort.members").isEqualTo(2)
		assertThat(result).bodyJson().extractingPath("$cohort.points[0].activeMembers").isEqualTo(2)
		assertThat(result).bodyJson().extractingPath("$cohort.points[0].retentionRate").isEqualTo(100.0)
		assertThat(result).bodyJson().extractingPath("$cohort.points[0].partial").isEqualTo(false)
		assertThat(result).bodyJson().extractingPath("$cohort.points[1].activeMembers").isEqualTo(1)
		assertThat(result).bodyJson().extractingPath("$cohort.points[1].retentionRate").isEqualTo(50.0)
		assertThat(result).bodyJson().extractingPath("$cohort.points[1].partial").isEqualTo(true)
		assertThat(result).bodyJson().extractingPath("$cohort.points[2].activeMembers").isNull()
		assertThat(result).bodyJson().extractingPath("$cohort.points[2].retentionRate").isNull()
		assertThat(result).bodyJson().extractingPath("$.data.cohorts[1].members").isEqualTo(0)
	}

	@Test
	@DisplayName("월 단위 코호트는 달력 월 경계로 offset을 나눈다")
	fun getMonthlyCohortRetention() {
		val thisMonthStart = today.withDayOfMonth(1)
		val lastMonthStart = thisMonthStart.minusMonths(1)
		val lastMonthEnd = thisMonthStart.minusDays(1)
		val member = userTestFactory.persistUser()
		memberStatisticsTestFactory.updateSignedUpAt(member.id, lastMonthStart.plusDays(5).atTime(10, 0))
		memberStatisticsTestFactory.persistDailyActivity(member.id, lastMonthEnd)
		memberStatisticsTestFactory.persistDailyActivity(member.id, thisMonthStart)

		val result = cohortRetention(lastMonthStart, today, "MONTH", 2)

		assertThat(result).hasStatusOk()
		assertThat(result).bodyJson().extractingPath("$.data.cohorts.length()").isEqualTo(2)
		assertThat(result).bodyJson().extractingPath("$.data.cohorts[0].cohortAt").isEqualTo("${lastMonthStart}T00:00:00")
		assertThat(result).bodyJson().extractingPath("$.data.cohorts[0].members").isEqualTo(1)
		assertThat(result).bodyJson().extractingPath("$.data.cohorts[0].points[0].activeMembers").isEqualTo(1)
		assertThat(result).bodyJson().extractingPath("$.data.cohorts[0].points[1].activeMembers").isEqualTo(1)
		assertThat(result).bodyJson().extractingPath("$.data.cohorts[0].points[1].partial").isEqualTo(true)
	}

	@Test
	@DisplayName("롤업이 비어 있으면 coverageFrom은 null이다")
	fun coverageFromIsNullWithoutRollup() {
		val result = cohortRetention(today, today, "DAY", 1)

		assertThat(result).hasStatusOk()
		assertThat(result).bodyJson().extractingPath("$.data.coverageFrom").isNull()
	}

	@Test
	@DisplayName("퍼널 구간이 91일이면 400을 반환한다")
	fun rejectFunnelRangeLongerThan90Days() {
		assertThat(funnel(today.minusDays(90), today)).hasStatus(HttpStatus.BAD_REQUEST)
	}

	@Test
	@DisplayName("코호트 구간이 367일이면 400을 반환한다")
	fun rejectCohortRangeLongerThan366Days() {
		assertThat(cohortRetention(today.minusDays(366), today, "WEEK", 1)).hasStatus(HttpStatus.BAD_REQUEST)
	}

	private fun funnel(from: LocalDate, to: LocalDate): MvcTestResult = mockMvcTester
		.get()
		.uri("/v1/statistics/members/funnel")
		.param("from", from.toString())
		.param("to", to.toString())
		.header("Authorization", "Bearer $accessToken")
		.exchange()

	private fun cohortRetention(from: LocalDate, to: LocalDate, granularity: String, offsets: Int): MvcTestResult = mockMvcTester
		.get()
		.uri("/v1/statistics/members/cohort-retention")
		.param("from", from.toString())
		.param("to", to.toString())
		.param("granularity", granularity)
		.param("offsets", offsets.toString())
		.header("Authorization", "Bearer $accessToken")
		.exchange()

	private fun persistVisit(
		visitorId: String,
		occurredAt: LocalDateTime,
		userId: Long? = null,
		ip: String?,
		deviceType: String = "모바일"
	) {
		visitorTelemetryTestFactory.persistEvent(occurredAt, visitorId, userId, ip, deviceType)
	}
}
