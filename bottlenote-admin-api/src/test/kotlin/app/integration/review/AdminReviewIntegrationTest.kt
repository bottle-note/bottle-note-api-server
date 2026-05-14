package app.integration.review

import app.IntegrationTestSupport
import app.bottlenote.alcohols.domain.Alcohol
import app.bottlenote.alcohols.fixture.AlcoholTestFactory
import app.bottlenote.global.service.cursor.SortOrder
import app.bottlenote.review.constant.AdminReviewSortType
import app.bottlenote.review.constant.ReviewActiveStatus
import app.bottlenote.review.constant.ReviewDisplayStatus
import app.bottlenote.review.domain.Review
import app.bottlenote.review.fixture.ReviewTestFactory
import app.bottlenote.user.domain.User
import app.bottlenote.user.fixture.UserTestFactory
import com.fasterxml.jackson.databind.JsonNode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

@Tag("admin_integration")
@DisplayName("[integration] Admin Review API 통합 테스트")
class AdminReviewIntegrationTest : IntegrationTestSupport() {
	@Autowired
	private lateinit var alcoholTestFactory: AlcoholTestFactory

	@Autowired
	private lateinit var userTestFactory: UserTestFactory

	@Autowired
	private lateinit var reviewTestFactory: ReviewTestFactory

	private lateinit var accessToken: String

	@BeforeEach
	fun setUp() {
		val admin = adminUserTestFactory.persistRootAdmin()
		accessToken = getAccessToken(admin)
	}

	@Test
	@DisplayName("기본 목록을 조회할 때 응답 필드와 전체 상태를 노출한다")
	fun searchAdminReviews_returnsFieldsAndAllStatuses() {
		// given
		seedReviews()

		// when
		val response = getReviews()

		// then
		assertThat(response.get("success").asBoolean()).isTrue()
		assertThat(response.get("data")).hasSize(3)
		assertThat(response.get("data").flatMap { it.fieldNames().asSequence().toList() })
			.contains(
				"reviewId",
				"alcoholId",
				"alcoholName",
				"userId",
				"userNickname",
				"content",
				"reviewRating",
				"activeStatus",
				"displayStatus",
				"replyCount",
				"createAt",
				"lastModifyAt"
			)
		assertThat(values(response, "activeStatus"))
			.containsExactlyInAnyOrder("ACTIVE", "DELETED", "DISABLED")
		assertThat(values(response, "displayStatus"))
			.containsExactlyInAnyOrder("PUBLIC", "PRIVATE", "PUBLIC")
	}

	@Test
	@DisplayName("필터 7종을 단독 적용할 때 결과를 좁힌다")
	fun searchAdminReviews_filtersBySingleCondition() {
		// given
		val fixture = seedReviews()

		// when & then
		assertReviewIds(getReviews("alcoholId" to fixture.alcoholA.id.toString()), fixture.activePublic.id, fixture.disabledPublic.id)
		assertReviewIds(getReviews("userId" to fixture.userB.id.toString()), fixture.deletedPrivate.id)
		assertReviewIds(getReviews("activeStatus" to "DELETED"), fixture.deletedPrivate.id)
		assertReviewIds(getReviews("displayStatus" to "PRIVATE"), fixture.deletedPrivate.id)
		assertReviewIds(getReviews("keyword" to "AlphaReviewer"), fixture.activePublic.id)
		assertReviewIds(getReviews("createdFrom" to "2026-01-02T00:00:00"), fixture.activePublic.id, fixture.deletedPrivate.id)
		assertReviewIds(getReviews("createdTo" to "2026-01-02T23:59:59"), fixture.deletedPrivate.id, fixture.disabledPublic.id)
	}

	@Test
	@DisplayName("키워드를 리뷰 본문, 작성자 이메일, 주류명에 적용한다")
	fun searchAdminReviews_filtersByKeywordTargets() {
		// given
		val fixture = seedReviews()

		// when & then
		assertReviewIds(getReviews("keyword" to "smoky keyword"), fixture.activePublic.id)
		assertReviewIds(getReviews("keyword" to "beta-admin"), fixture.deletedPrivate.id)
		assertReviewIds(getReviews("keyword" to "Admin Macallan"), fixture.deletedPrivate.id)
		assertReviewIds(getReviews("keyword" to "Admin Ardbeg"), fixture.activePublic.id, fixture.disabledPublic.id)
	}

	@Test
	@DisplayName("정렬 조건과 방향을 적용한다")
	fun searchAdminReviews_sortsByTypeAndOrder() {
		// given
		val fixture = seedReviews()

		// when & then
		assertReviewIds(sorted(AdminReviewSortType.CREATED_AT, SortOrder.DESC), fixture.activePublic.id, fixture.deletedPrivate.id, fixture.disabledPublic.id)
		assertReviewIds(sorted(AdminReviewSortType.CREATED_AT, SortOrder.ASC), fixture.disabledPublic.id, fixture.deletedPrivate.id, fixture.activePublic.id)
		assertReviewIds(sorted(AdminReviewSortType.REPLY_COUNT, SortOrder.DESC), fixture.activePublic.id, fixture.disabledPublic.id, fixture.deletedPrivate.id)
		assertReviewIds(sorted(AdminReviewSortType.REPLY_COUNT, SortOrder.ASC), fixture.deletedPrivate.id, fixture.disabledPublic.id, fixture.activePublic.id)
		assertReviewIds(sorted(AdminReviewSortType.UPDATED_AT, SortOrder.DESC), fixture.deletedPrivate.id, fixture.activePublic.id, fixture.disabledPublic.id)
		assertReviewIds(sorted(AdminReviewSortType.UPDATED_AT, SortOrder.ASC), fixture.disabledPublic.id, fixture.activePublic.id, fixture.deletedPrivate.id)
	}

	@Test
	@DisplayName("페이지 메타 정보를 반환한다")
	fun searchAdminReviews_returnsPageMeta() {
		// given
		seedReviews()

		// when
		val response = getReviews("page" to "0", "size" to "2")

		// then
		assertThat(response.get("data")).hasSize(2)
		assertThat(response.at("/meta/page").asInt()).isEqualTo(0)
		assertThat(response.at("/meta/size").asInt()).isEqualTo(2)
		assertThat(response.at("/meta/totalElements").asLong()).isEqualTo(3)
		assertThat(response.at("/meta/totalPages").asInt()).isEqualTo(2)
		assertThat(response.at("/meta/hasNext").asBoolean()).isTrue()
	}

	private fun sorted(
		sortType: AdminReviewSortType,
		sortOrder: SortOrder
	): JsonNode = getReviews("sortType" to sortType.name, "sortOrder" to sortOrder.name)

	private fun getReviews(vararg params: Pair<String, String>): JsonNode {
		val request = mockMvcTester
			.get()
			.uri("/reviews")
			.header("Authorization", "Bearer $accessToken")
		params.forEach { request.param(it.first, it.second) }

		val result = request.exchange()
		assertThat(result).hasStatusOk()
		return mapper.readTree(result.response.contentAsString)
	}

	private fun assertReviewIds(
		response: JsonNode,
		vararg expected: Long
	) {
		assertThat(reviewIds(response)).containsExactly(*expected.toTypedArray())
	}

	private fun reviewIds(response: JsonNode): List<Long> = response.get("data").map { it.get("reviewId").asLong() }

	private fun values(
		response: JsonNode,
		fieldName: String
	): List<String> = response.get("data").map { it.get(fieldName).asText() }

	private fun seedReviews(): SeededReviews {
		val userA = userTestFactory.persistUser("alpha-admin", "AlphaReviewer")
		val userB = userTestFactory.persistUser("beta-admin", "BetaReviewer")
		val userC = userTestFactory.persistUser("gamma-admin", "GammaReviewer")
		val alcoholA = alcoholTestFactory.persistAlcoholWithName("어드민 아드벡", "Admin Ardbeg")
		val alcoholB = alcoholTestFactory.persistAlcoholWithName("어드민 맥캘란", "Admin Macallan")

		val disabledPublic = reviewTestFactory.persistAdminReview(
			userC,
			alcoholA,
			"disabled oak note",
			ReviewActiveStatus.DISABLED,
			ReviewDisplayStatus.PUBLIC,
			3.0,
			LocalDateTime.of(2026, 1, 1, 10, 0),
			LocalDateTime.of(2026, 1, 4, 10, 0)
		)
		val deletedPrivate = reviewTestFactory.persistAdminReview(
			userB,
			alcoholB,
			"deleted private peat note",
			ReviewActiveStatus.DELETED,
			ReviewDisplayStatus.PRIVATE,
			4.0,
			LocalDateTime.of(2026, 1, 2, 10, 0),
			LocalDateTime.of(2026, 1, 6, 10, 0)
		)
		val activePublic = reviewTestFactory.persistAdminReview(
			userA,
			alcoholA,
			"active smoky keyword note",
			ReviewActiveStatus.ACTIVE,
			ReviewDisplayStatus.PUBLIC,
			4.5,
			LocalDateTime.of(2026, 1, 3, 10, 0),
			LocalDateTime.of(2026, 1, 5, 10, 0)
		)
		reviewTestFactory.persistReviewReply(activePublic, userB)
		reviewTestFactory.persistReviewReply(activePublic, userC)
		reviewTestFactory.persistReviewReply(disabledPublic, userA)

		return SeededReviews(userA, userB, userC, alcoholA, alcoholB, activePublic, deletedPrivate, disabledPublic)
	}

	private data class SeededReviews(
		val userA: User,
		val userB: User,
		val userC: User,
		val alcoholA: Alcohol,
		val alcoholB: Alcohol,
		val activePublic: Review,
		val deletedPrivate: Review,
		val disabledPublic: Review
	)
}
