package app.integration.review

import app.IntegrationTestSupport
import app.bottlenote.alcohols.fixture.AlcoholTestFactory
import app.bottlenote.review.constant.BestReviewChangeType
import app.bottlenote.review.domain.Review
import app.bottlenote.review.fixture.BestReviewSelectionLogTestFactory
import app.bottlenote.review.fixture.ReviewTestFactory
import app.bottlenote.user.fixture.UserTestFactory
import com.fasterxml.jackson.databind.JsonNode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate

@Tag("admin_integration")
@DisplayName("[integration] Admin 베스트 리뷰 선정 로그 API 통합 테스트")
class AdminBestReviewSelectionLogIntegrationTest : IntegrationTestSupport() {
	@Autowired
	private lateinit var alcoholTestFactory: AlcoholTestFactory

	@Autowired
	private lateinit var userTestFactory: UserTestFactory

	@Autowired
	private lateinit var reviewTestFactory: ReviewTestFactory

	@Autowired
	private lateinit var logTestFactory: BestReviewSelectionLogTestFactory

	private lateinit var accessToken: String

	@BeforeEach
	fun setUp() {
		val admin = adminUserTestFactory.persistRootAdmin()
		accessToken = getAccessToken(admin)
	}

	@Test
	@DisplayName("목록을 조회할 때 리뷰·주류·작성자 정보와 사유를 최근 기준일부터 내린다")
	fun searchLogs_returnsFieldsOrderedByLatestDate() {
		// given
		val fixture = seedLogs()

		// when
		val response = getLogs()

		// then
		assertThat(response.get("success").asBoolean()).isTrue()
		assertThat(logIds(response)).containsExactly(fixture.releasedOnDay2, fixture.selectedOnDay2, fixture.selectedOnDay1)
		val latest = response.get("data").first()
		assertThat(latest.get("reviewId").asLong()).isEqualTo(fixture.reviewA.id)
		assertThat(latest.get("alcoholName").asText()).isEqualTo("어드민 스프링뱅크")
		assertThat(latest.get("userNickname").asText()).startsWith("LogReviewer")
		assertThat(latest.get("changeType").asText()).isEqualTo("RELEASED")
		assertThat(latest.get("ranking").asInt()).isEqualTo(3)
		assertThat(BigDecimal(latest.get("score").asText())).isEqualByComparingTo("1.10")
		assertThat(latest.get("likeCount").asLong()).isEqualTo(5)
		assertThat(latest.get("ruleCode").asText()).isEqualTo("REVIEW_COUNT_10_TO_19_TOP_2")
		assertThat(latest.get("reason").asText()).contains("해제")
		assertThat(latest.get("selectionDate").asText()).isEqualTo("2026-09-24")
		assertThat(latest.get("createdAt").isNull).isFalse()
	}

	@Test
	@DisplayName("리뷰·주류·변동 종류·기준일 범위로 좁힌다")
	fun searchLogs_filters() {
		// given
		val fixture = seedLogs()

		// when & then
		assertLogIds(getLogs("reviewId" to fixture.reviewB.id.toString()), fixture.selectedOnDay2)
		assertLogIds(getLogs("alcoholId" to fixture.reviewA.alcoholId.toString()), fixture.releasedOnDay2, fixture.selectedOnDay1)
		assertLogIds(getLogs("changeType" to "SELECTED"), fixture.selectedOnDay2, fixture.selectedOnDay1)
		assertLogIds(getLogs("selectedFrom" to "2026-09-24"), fixture.releasedOnDay2, fixture.selectedOnDay2)
		assertLogIds(getLogs("selectedTo" to "2026-09-23"), fixture.selectedOnDay1)
	}

	@Test
	@DisplayName("페이지 메타 정보를 반환하고 범위를 벗어난 페이지 요청은 400이다")
	fun searchLogs_pagination() {
		// given
		seedLogs()

		// when
		val response = getLogs("page" to "0", "size" to "2")

		// then
		assertThat(response.get("data")).hasSize(2)
		assertThat(response.at("/meta/totalElements").asLong()).isEqualTo(3)
		assertThat(response.at("/meta/hasNext").asBoolean()).isTrue()
		assertInvalidSearch("page" to "-1")
		assertInvalidSearch("size" to "0")
		assertInvalidSearch("size" to "101")
	}

	private fun getLogs(vararg params: Pair<String, String>): JsonNode {
		val request = mockMvcTester
			.get()
			.uri("/v1/reviews/best-selection-logs")
			.header("Authorization", "Bearer $accessToken")
		params.forEach { request.param(it.first, it.second) }

		val result = request.exchange()
		assertThat(result).hasStatusOk()
		return mapper.readTree(result.response.contentAsString)
	}

	private fun assertInvalidSearch(vararg params: Pair<String, String>) {
		val request = mockMvcTester
			.get()
			.uri("/v1/reviews/best-selection-logs")
			.header("Authorization", "Bearer $accessToken")
		params.forEach { request.param(it.first, it.second) }

		assertThat(request.exchange()).hasStatus(400)
	}

	private fun assertLogIds(
		response: JsonNode,
		vararg expected: Long
	) {
		assertThat(logIds(response)).containsExactly(*expected.toTypedArray())
	}

	private fun logIds(response: JsonNode): List<Long> = response.get("data").map { it.get("logId").asLong() }

	private fun seedLogs(): SeededLogs {
		val user = userTestFactory.persistUser("log-admin", "LogReviewer")
		val alcoholA = alcoholTestFactory.persistAlcoholWithName("어드민 스프링뱅크", "Admin Springbank")
		val alcoholB = alcoholTestFactory.persistAlcoholWithName("어드민 라가불린", "Admin Lagavulin")
		val reviewA = reviewTestFactory.persistReview(user, alcoholA)
		val reviewB = reviewTestFactory.persistReview(user, alcoholB)

		val day1 = LocalDate.of(2026, 9, 23)
		val day2 = LocalDate.of(2026, 9, 24)
		val selectedOnDay1 = logTestFactory.persistLog(reviewA, day1, BestReviewChangeType.SELECTED)
		val selectedOnDay2 = logTestFactory.persistLog(reviewB, day2, BestReviewChangeType.SELECTED)
		val releasedOnDay2 = logTestFactory.persistLog(reviewA, day2, BestReviewChangeType.RELEASED, 3, BigDecimal("1.10"))

		return SeededLogs(reviewA, reviewB, selectedOnDay1.id, selectedOnDay2.id, releasedOnDay2.id)
	}

	private data class SeededLogs(
		val reviewA: Review,
		val reviewB: Review,
		val selectedOnDay1: Long,
		val selectedOnDay2: Long,
		val releasedOnDay2: Long
	)
}
