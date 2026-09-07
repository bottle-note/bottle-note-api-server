package app.integration.statistics

import app.IntegrationTestSupport
import app.bottlenote.alcohols.constant.BucketGranularity
import app.bottlenote.alcohols.fixture.AlcoholPopularityTestFactory
import app.bottlenote.alcohols.fixture.AlcoholTestFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@Tag("admin_integration")
@DisplayName("[integration] Admin 주류 인기도 시계열 API")
class AdminAlcoholStatisticsIntegrationTest : IntegrationTestSupport() {
	@Autowired
	private lateinit var alcoholTestFactory: AlcoholTestFactory

	@Autowired
	private lateinit var popularityTestFactory: AlcoholPopularityTestFactory

	private lateinit var accessToken: String
	private val seoul: ZoneId = ZoneId.of("Asia/Seoul")

	@BeforeEach
	fun setUp() {
		val admin = adminUserTestFactory.persistRootAdmin()
		accessToken = getAccessToken(admin)
	}

	@Test
	@DisplayName("popularity WEEK는 닫힌 두 포인트 값과 열린 버킷의 HOUR 합을 내린다")
	fun popularityWeekReturnsClosedPointsAndOpenRolldown() {
		val alcohol = alcoholTestFactory.persistAlcohol()
		val now = LocalDateTime.now(seoul)
		val openWeek = BucketGranularity.WEEK.startAt(now)
		val closed2 = openWeek.minusWeeks(1)
		val closed1 = openWeek.minusWeeks(2)
		popularityTestFactory.persistSnapshot(
			alcohol.id,
			BucketGranularity.WEEK,
			closed1,
			1L,
			10L,
			2L,
			3L,
			BigDecimal("0.10"),
			BigDecimal("0.20"),
			BigDecimal("0.30"),
			BigDecimal("0.40"),
			BigDecimal("0.50")
		)
		popularityTestFactory.persistSnapshot(
			alcohol.id,
			BucketGranularity.WEEK,
			closed2,
			2L,
			11L,
			3L,
			4L,
			BigDecimal("0.11"),
			BigDecimal("0.21"),
			BigDecimal("0.31"),
			BigDecimal("0.41"),
			BigDecimal("0.51")
		)
		popularityTestFactory.persistSnapshot(
			alcohol.id,
			BucketGranularity.HOUR,
			openWeek.plusHours(1),
			4L,
			12L,
			5L,
			6L,
			BigDecimal("0.01"),
			BigDecimal("0.02"),
			BigDecimal("0.03"),
			BigDecimal("0.04"),
			BigDecimal("0.05")
		)
		popularityTestFactory.persistSnapshot(
			alcohol.id,
			BucketGranularity.HOUR,
			openWeek.plusHours(2),
			6L,
			13L,
			7L,
			8L,
			BigDecimal("0.06"),
			BigDecimal("0.07"),
			BigDecimal("0.08"),
			BigDecimal("0.09"),
			BigDecimal("0.10")
		)

		val result =
			mockMvcTester
				.get()
				.uri("/v1/statistics/alcohols/{alcoholId}/popularity", alcohol.id)
				.header("Authorization", "Bearer $accessToken")
				.param("from", closed1.toLocalDate().toString())
				.param("to", now.toLocalDate().toString())
				.param("granularity", "WEEK")
				.exchange()

		result.assertThat().hasStatusOk()
		val points = mapper.readTree(result.response.contentAsString).path("data").path("points")
		assertThat(points.size()).isEqualTo(3)
		assertThat(points[0].path("partial").asBoolean()).isFalse()
		assertThat(points[0].path("values").path("interestValue").asLong()).isEqualTo(1L)
		assertThat(points[0].path("values").path("popularityScore").asDouble()).isEqualTo(0.5)
		assertThat(points[1].path("partial").asBoolean()).isFalse()
		assertThat(points[1].path("values").path("interestValue").asLong()).isEqualTo(2L)
		assertThat(points[2].path("partial").asBoolean()).isTrue()
		assertThat(points[2].path("values").path("interestValue").asLong()).isEqualTo(10L)
		assertThat(points[2].path("values").path("popularityScore").isNull).isTrue()
		assertThat(points[2].path("values").path("interestScore").isNull).isTrue()
	}

	@Test
	@DisplayName("observations/RATING WEEK는 롤다운한 평균 평점을 내린다")
	fun ratingWeekReturnsRolledDownAverage() {
		val alcohol = alcoholTestFactory.persistAlcohol()
		val now = LocalDateTime.now(seoul)
		val openWeek = BucketGranularity.WEEK.startAt(now)
		val closed = openWeek.minusWeeks(1)
		popularityTestFactory.persistRating(
			alcohol.id,
			BucketGranularity.WEEK,
			closed,
			4L,
			BigDecimal("16.0"),
			4L,
			BigDecimal("16.0")
		)
		popularityTestFactory.persistRating(
			alcohol.id,
			BucketGranularity.HOUR,
			openWeek.plusHours(1),
			8L,
			BigDecimal("32.0"),
			1L,
			BigDecimal("4.0")
		)
		popularityTestFactory.persistRating(
			alcohol.id,
			BucketGranularity.HOUR,
			openWeek.plusHours(2),
			10L,
			BigDecimal("45.0"),
			2L,
			BigDecimal("13.0")
		)

		val result =
			mockMvcTester
				.get()
				.uri("/v1/statistics/alcohols/{alcoholId}/observations/{axis}", alcohol.id, "RATING")
				.header("Authorization", "Bearer $accessToken")
				.param("from", closed.toLocalDate().toString())
				.param("to", now.toLocalDate().toString())
				.param("granularity", "WEEK")
				.exchange()

		result.assertThat().hasStatusOk()
		val last = mapper.readTree(result.response.contentAsString).path("data").path("points").last()
		assertThat(last.path("partial").asBoolean()).isTrue()
		assertThat(last.path("values").path("averageRating").asDouble()).isEqualTo(4.5)
	}

	@Test
	@DisplayName("없는 주류는 404다")
	fun missingAlcoholReturnsNotFound() {
		assertThat(
			mockMvcTester
				.get()
				.uri("/v1/statistics/alcohols/{alcoholId}/popularity", 999999999L)
				.header("Authorization", "Bearer $accessToken")
				.param("from", LocalDate.now(seoul).minusWeeks(2).toString())
				.param("to", LocalDate.now(seoul).toString())
				.param("granularity", "WEEK")
		).hasStatus(HttpStatus.NOT_FOUND)
	}

	@Test
	@DisplayName("granularity=DAY는 400이다")
	fun dayGranularityReturnsBadRequest() {
		val alcohol = alcoholTestFactory.persistAlcohol()
		assertThat(
			mockMvcTester
				.get()
				.uri("/v1/statistics/alcohols/{alcoholId}/popularity", alcohol.id)
				.header("Authorization", "Bearer $accessToken")
				.param("granularity", "DAY")
		).hasStatus(HttpStatus.BAD_REQUEST)
	}

	@Test
	@DisplayName("토큰 없는 요청은 기존 Admin 보안 정책대로 403이다")
	fun missingTokenReturnsForbidden() {
		assertThat(
			mockMvcTester
				.get()
				.uri("/v1/statistics/alcohols/{alcoholId}/popularity", 1L)
		).hasStatus(HttpStatus.FORBIDDEN)
	}
}
