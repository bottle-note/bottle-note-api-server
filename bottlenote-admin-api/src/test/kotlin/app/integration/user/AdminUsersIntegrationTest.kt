package app.integration.user

import app.IntegrationTestSupport
import app.bottlenote.alcohols.fixture.AlcoholTestFactory
import app.bottlenote.picks.fixture.PicksTestFactory
import app.bottlenote.rating.fixture.RatingTestFactory
import app.bottlenote.review.fixture.ReviewTestFactory
import app.bottlenote.user.fixture.UserTestFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@Tag("admin_integration")
@DisplayName("[integration] Admin Users API 통합 테스트")
class AdminUsersIntegrationTest : IntegrationTestSupport() {

	@Autowired
	private lateinit var userTestFactory: UserTestFactory

	@Autowired
	private lateinit var alcoholTestFactory: AlcoholTestFactory

	@Autowired
	private lateinit var reviewTestFactory: ReviewTestFactory

	@Autowired
	private lateinit var ratingTestFactory: RatingTestFactory

	@Autowired
	private lateinit var picksTestFactory: PicksTestFactory

	private lateinit var accessToken: String

	@BeforeEach
	fun setUp() {
		val admin = adminUserTestFactory.persistRootAdmin()
		accessToken = getAccessToken(admin)
	}

	@Nested
	@DisplayName("유저 목록 조회 API")
	inner class ListUsers {

		@Test
		@DisplayName("유저 목록을 조회할 수 있다")
		fun listSuccess() {
			// given
			userTestFactory.persistUser()
			userTestFactory.persistUser()

			// when & then
			assertThat(
				mockMvcTester
					.get()
					.uri("/users")
					.header("Authorization", "Bearer $accessToken")
			).hasStatusOk()
				.bodyJson()
				.extractingPath("$.success")
				.isEqualTo(true)
		}

		@Test
		@DisplayName("키워드로 닉네임을 검색할 수 있다")
		fun searchByNickname() {
			// given
			userTestFactory.persistUserWithNickname("검색대상유저")
			userTestFactory.persistUser()

			// when & then
			assertThat(
				mockMvcTester
					.get()
					.uri("/users")
					.param("keyword", "검색대상")
					.header("Authorization", "Bearer $accessToken")
			).hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.length()")
				.isEqualTo(1)
		}

		@Test
		@DisplayName("키워드로 이메일을 검색할 수 있다")
		fun searchByEmail() {
			// given
			userTestFactory.persistUser("searchtarget", "이메일유저")
			userTestFactory.persistUser()

			// when & then
			assertThat(
				mockMvcTester
					.get()
					.uri("/users")
					.param("keyword", "searchtarget")
					.header("Authorization", "Bearer $accessToken")
			).hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.length()")
				.isEqualTo(1)
		}

		@Test
		@DisplayName("상태로 필터링할 수 있다")
		fun filterByStatus() {
			// given
			userTestFactory.persistUser()
			val deletedUser = userTestFactory.persistUser()
			deletedUser.withdrawUser()

			// when & then
			assertThat(
				mockMvcTester
					.get()
					.uri("/users")
					.param("status", "ACTIVE")
					.header("Authorization", "Bearer $accessToken")
			).hasStatusOk()
				.bodyJson()
				.extractingPath("$.success")
				.isEqualTo(true)
		}

		@Test
		@DisplayName("활동 지표(리뷰, 별점, 찜)가 포함된다")
		fun includesActivityMetrics() {
			// given
			val user = userTestFactory.persistUser()
			val alcohol = alcoholTestFactory.persistAlcohol()

			reviewTestFactory.persistReview(user, alcohol)
			reviewTestFactory.persistReview(user, alcoholTestFactory.persistAlcohol())
			ratingTestFactory.persistRating(user, alcohol, 4)
			picksTestFactory.persistPicks(alcohol.id, user.id)

			// when
			val result = mockMvcTester
				.get()
				.uri("/users")
				.param("keyword", user.nickName)
				.header("Authorization", "Bearer $accessToken")
				.exchange()

			// then
			assertThat(result).hasStatusOk()
			assertThat(result)
				.bodyJson()
				.extractingPath("$.data[0].reviewCount")
				.isEqualTo(2)
			assertThat(result)
				.bodyJson()
				.extractingPath("$.data[0].ratingCount")
				.isEqualTo(1)
			assertThat(result)
				.bodyJson()
				.extractingPath("$.data[0].picksCount")
				.isEqualTo(1)
		}

		@Test
		@DisplayName("정렬 옵션이 동작한다")
		fun sortByReviewCount() {
			// given
			userTestFactory.persistUser() // userA (리뷰 없음)
			val userB = userTestFactory.persistUser()
			val alcohol = alcoholTestFactory.persistAlcohol()

			reviewTestFactory.persistReview(userB, alcohol)
			reviewTestFactory.persistReview(userB, alcoholTestFactory.persistAlcohol())

			// when & then (리뷰 많은 순 DESC -> userB가 먼저)
			val result = mockMvcTester
				.get()
				.uri("/users")
				.param("sortType", "REVIEW_COUNT")
				.param("sortOrder", "DESC")
				.header("Authorization", "Bearer $accessToken")
				.exchange()

			assertThat(result).hasStatusOk()
			assertThat(result)
				.bodyJson()
				.extractingPath("$.data[0].userId")
				.isEqualTo(userB.id.toInt())
		}

		@Test
		@DisplayName("페이징이 동작한다")
		fun pagination() {
			// given
			repeat(5) { userTestFactory.persistUser() }

			// when & then
			assertThat(
				mockMvcTester
					.get()
					.uri("/users")
					.param("page", "0")
					.param("size", "2")
					.header("Authorization", "Bearer $accessToken")
			).hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.length()")
				.isEqualTo(2)
		}
	}

	@Nested
	@DisplayName("인증 테스트")
	inner class AuthenticationTest {
		@Test
		@DisplayName("인증 없이 요청하면 실패한다")
		fun requestWithoutAuth() {
			assertThat(
				mockMvcTester.get().uri("/users")
			).hasStatus4xxClientError()
		}
	}
}
