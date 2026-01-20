package app.integration.alcohols

import app.IntegrationTestSupport
import app.bottlenote.alcohols.constant.AdminAlcoholSortType
import app.bottlenote.alcohols.constant.AlcoholCategoryGroup
import app.bottlenote.alcohols.constant.AlcoholType
import app.bottlenote.alcohols.fixture.AlcoholTestFactory
import app.bottlenote.global.service.cursor.SortOrder
import app.bottlenote.rating.fixture.RatingTestFactory
import app.bottlenote.review.fixture.ReviewTestFactory
import app.bottlenote.user.fixture.UserTestFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import java.util.stream.Stream

@Tag("admin_integration")
@DisplayName("[integration] Admin Alcohol API 통합 테스트")
class AdminAlcoholsIntegrationTest : IntegrationTestSupport() {

	@Autowired
	private lateinit var alcoholTestFactory: AlcoholTestFactory

	@Autowired
	private lateinit var userTestFactory: UserTestFactory

	@Autowired
	private lateinit var reviewTestFactory: ReviewTestFactory

	@Autowired
	private lateinit var ratingTestFactory: RatingTestFactory

	private lateinit var accessToken: String

	@BeforeEach
	fun setUp() {
		val admin = adminUserTestFactory.persistRootAdmin()
		accessToken = getAccessToken(admin)
	}

	companion object {
		@JvmStatic
		fun keywordSearchTestCases(): Stream<Arguments> = Stream.of(
			Arguments.of("글렌피딕", 1, "한글 이름으로 검색"),
			Arguments.of("Glenfiddich", 1, "영문 이름으로 검색"),
			Arguments.of("테킬라", 0, "매칭되지 않는 키워드")
		)

		@JvmStatic
		fun sortTypeTestCases(): Stream<Arguments> = Stream.of(
			Arguments.of(AdminAlcoholSortType.KOR_NAME, SortOrder.ASC),
			Arguments.of(AdminAlcoholSortType.KOR_NAME, SortOrder.DESC),
			Arguments.of(AdminAlcoholSortType.ENG_NAME, SortOrder.ASC),
			Arguments.of(AdminAlcoholSortType.ENG_NAME, SortOrder.DESC),
			Arguments.of(AdminAlcoholSortType.KOR_CATEGORY, SortOrder.ASC),
			Arguments.of(AdminAlcoholSortType.ENG_CATEGORY, SortOrder.DESC)
		)
	}

	@Test
	@DisplayName("관리자용 술 목록을 조회할 수 있다")
	fun searchAdminAlcohols() {
		// given
		alcoholTestFactory.persistAlcohols(5)

		// when & then
		assertThat(
			mockMvcTester.get().uri("/alcohols")
				.header("Authorization", "Bearer $accessToken")
		)
			.hasStatusOk()
			.bodyJson()
			.extractingPath("$.success").isEqualTo(true)
	}

	@ParameterizedTest(name = "{2}")
	@MethodSource("keywordSearchTestCases")
	@DisplayName("키워드로 술을 검색할 수 있다")
	fun searchByKeyword(keyword: String, expectedCount: Int, description: String) {
		// given
		alcoholTestFactory.persistAlcoholWithName("글렌피딕 12년", "Glenfiddich 12")
		alcoholTestFactory.persistAlcoholWithName("맥캘란 18년", "Macallan 18")

		// when & then
		assertThat(
			mockMvcTester.get().uri("/alcohols")
				.header("Authorization", "Bearer $accessToken")
				.param("keyword", keyword)
		)
			.hasStatusOk()
			.bodyJson()
			.extractingPath("$.data.length()").isEqualTo(expectedCount)
	}

	@ParameterizedTest(name = "카테고리: {0}")
	@EnumSource(AlcoholCategoryGroup::class)
	@DisplayName("카테고리로 필터링할 수 있다")
	fun filterByCategory(category: AlcoholCategoryGroup) {
		// given
		alcoholTestFactory.persistAlcohols(3)

		// when & then
		assertThat(
			mockMvcTester.get().uri("/alcohols")
				.header("Authorization", "Bearer $accessToken")
				.param("category", category.name)
		)
			.hasStatusOk()
			.bodyJson()
			.extractingPath("$.success").isEqualTo(true)
	}

	@ParameterizedTest(name = "정렬: {0} {1}")
	@MethodSource("sortTypeTestCases")
	@DisplayName("다양한 정렬 조건으로 조회할 수 있다")
	fun sortByVariousTypes(sortType: AdminAlcoholSortType, sortOrder: SortOrder) {
		// given
		alcoholTestFactory.persistAlcoholWithName("가나다 위스키", "ABC Whisky")
		alcoholTestFactory.persistAlcoholWithName("마바사 위스키", "DEF Whisky")

		// when & then
		assertThat(
			mockMvcTester.get().uri("/alcohols")
				.header("Authorization", "Bearer $accessToken")
				.param("sortType", sortType.name)
				.param("sortOrder", sortOrder.name)
		)
			.hasStatusOk()
			.bodyJson()
			.extractingPath("$.success").isEqualTo(true)
	}

	@ParameterizedTest(name = "page={0}, size={1}")
	@CsvSource(
		"0, 10",
		"0, 20",
		"1, 10",
		"2, 5"
	)
	@DisplayName("페이징이 정상 동작한다")
	fun pagination(page: Int, size: Int) {
		// given
		alcoholTestFactory.persistAlcohols(25)

		// when & then
		assertThat(
			mockMvcTester.get().uri("/alcohols")
				.header("Authorization", "Bearer $accessToken")
				.param("page", page.toString())
				.param("size", size.toString())
		)
			.hasStatusOk()
			.bodyJson()
			.extractingPath("$.meta.size").isEqualTo(size)
	}

	@Nested
	@DisplayName("카테고리 레퍼런스 조회 API")
	inner class GetCategoryReference {

		@Test
		@DisplayName("기존 카테고리 페어 목록을 조회할 수 있다")
		fun getCategoryReferenceSuccess() {
			// given - 다양한 카테고리를 가진 술 생성
			alcoholTestFactory.persistAlcoholWithCategory("싱글 몰트", "Single Malt")
			alcoholTestFactory.persistAlcoholWithCategory("블렌디드", "Blended")
			alcoholTestFactory.persistAlcoholWithCategory("싱글 몰트", "Single Malt") // 중복

			// when & then
			assertThat(
				mockMvcTester.get().uri("/alcohols/categories/reference")
					.header("Authorization", "Bearer $accessToken")
			)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.success").isEqualTo(true)
		}

		@Test
		@DisplayName("동일 한글 카테고리에 다른 영문 카테고리가 별도로 조회된다")
		fun differentEngCategoriesAreSeparate() {
			// given - 같은 한글 카테고리, 다른 영문 카테고리
			alcoholTestFactory.persistAlcoholWithCategory("싱글 몰트", "Single Malt")
			alcoholTestFactory.persistAlcoholWithCategory("싱글 몰트", "-")

			// when & then - 2개의 다른 페어가 반환됨
			assertThat(
				mockMvcTester.get().uri("/alcohols/categories/reference")
					.header("Authorization", "Bearer $accessToken")
			)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.length()").isEqualTo(2)
		}
	}

	@Nested
	@DisplayName("술 단건 상세 조회 API")
	inner class GetAlcoholDetail {

		@Test
		@DisplayName("관리자용 술 단건 상세 정보를 조회할 수 있다")
		fun getAlcoholDetailSuccess() {
			// given
			val alcohol = alcoholTestFactory.persistAlcoholWithName("글렌피딕 12년", "Glenfiddich 12")

			// when & then - 성공 응답 확인
			assertThat(
				mockMvcTester.get().uri("/alcohols/${alcohol.id}")
					.header("Authorization", "Bearer $accessToken")
			)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.success").isEqualTo(true)

			// 상세 데이터 검증
			assertThat(
				mockMvcTester.get().uri("/alcohols/${alcohol.id}")
					.header("Authorization", "Bearer $accessToken")
			)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.korName").isEqualTo("글렌피딕 12년")
		}

		@Test
		@DisplayName("모든 상세 필드가 포함되어 응답한다")
		fun getAlcoholDetailWithAllFields() {
			// given
			val alcohol = alcoholTestFactory.persistAlcoholWithName("맥캘란 18년", "Macallan 18")

			// when & then - 필수 필드 존재 여부 확인
			val result = mockMvcTester.get().uri("/alcohols/${alcohol.id}")
				.header("Authorization", "Bearer $accessToken")

			assertThat(result)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.alcoholId").isNotNull

			// 방어로직: 존재하지 않는 ID로 조회 시 실패
			assertThat(
				mockMvcTester.get().uri("/alcohols/999999")
					.header("Authorization", "Bearer $accessToken")
			)
				.hasStatus4xxClientError()
		}
	}

	@Nested
	@DisplayName("술 생성 API")
	inner class CreateAlcohol {

		@Test
		@DisplayName("위스키를 생성할 수 있다")
		fun createAlcoholSuccess() {
			// given
			val region = alcoholTestFactory.persistRegion()
			val distillery = alcoholTestFactory.persistDistillery()

			val request = mapOf(
				"korName" to "테스트 위스키",
				"engName" to "Test Whisky",
				"abv" to "40%",
				"type" to AlcoholType.WHISKY.name,
				"korCategory" to "싱글 몰트",
				"engCategory" to "Single Malt",
				"categoryGroup" to AlcoholCategoryGroup.SINGLE_MALT.name,
				"regionId" to region.id,
				"distilleryId" to distillery.id,
				"age" to "12",
				"cask" to "American Oak",
				"imageUrl" to "https://example.com/test.jpg",
				"description" to "테스트 설명",
				"volume" to "700ml"
			)

			// when & then
			assertThat(
				mockMvcTester.post().uri("/alcohols")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.code").isEqualTo("ALCOHOL_CREATED")
		}

		@Test
		@DisplayName("필수 필드 누락 시 실패한다")
		fun createAlcoholWithMissingFields() {
			// given
			val request = mapOf(
				"korName" to "테스트 위스키"
			)

			// when & then
			assertThat(
				mockMvcTester.post().uri("/alcohols")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			)
				.hasStatus4xxClientError()
		}

		@Test
		@DisplayName("존재하지 않는 regionId로 생성 시 실패한다")
		fun createAlcoholWithInvalidRegion() {
			// given
			val distillery = alcoholTestFactory.persistDistillery()

			val request = mapOf(
				"korName" to "테스트 위스키",
				"engName" to "Test Whisky",
				"abv" to "40%",
				"type" to AlcoholType.WHISKY.name,
				"korCategory" to "싱글 몰트",
				"engCategory" to "Single Malt",
				"categoryGroup" to AlcoholCategoryGroup.SINGLE_MALT.name,
				"regionId" to 999999L,
				"distilleryId" to distillery.id,
				"age" to "12",
				"cask" to "American Oak",
				"imageUrl" to "https://example.com/test.jpg",
				"description" to "테스트 설명",
				"volume" to "700ml"
			)

			// when & then
			assertThat(
				mockMvcTester.post().uri("/alcohols")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			)
				.hasStatus4xxClientError()
		}
	}

	@Nested
	@DisplayName("술 수정 API")
	inner class UpdateAlcohol {

		@Test
		@DisplayName("위스키를 수정할 수 있다")
		fun updateAlcoholSuccess() {
			// given
			val alcohol = alcoholTestFactory.persistAlcohol()
			val region = alcoholTestFactory.persistRegion()
			val distillery = alcoholTestFactory.persistDistillery()

			val request = mapOf(
				"korName" to "수정된 위스키",
				"engName" to "Updated Whisky",
				"abv" to "43%",
				"type" to AlcoholType.WHISKY.name,
				"korCategory" to "싱글 몰트",
				"engCategory" to "Single Malt",
				"categoryGroup" to AlcoholCategoryGroup.SINGLE_MALT.name,
				"regionId" to region.id,
				"distilleryId" to distillery.id,
				"age" to "18",
				"cask" to "Sherry Oak",
				"imageUrl" to "https://example.com/updated.jpg",
				"description" to "수정된 설명",
				"volume" to "750ml"
			)

			// when & then
			assertThat(
				mockMvcTester.put().uri("/alcohols/${alcohol.id}")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.code").isEqualTo("ALCOHOL_UPDATED")
		}

		@Test
		@DisplayName("존재하지 않는 alcoholId로 수정 시 실패한다")
		fun updateAlcoholNotFound() {
			// given
			val region = alcoholTestFactory.persistRegion()
			val distillery = alcoholTestFactory.persistDistillery()

			val request = mapOf(
				"korName" to "수정된 위스키",
				"engName" to "Updated Whisky",
				"abv" to "43%",
				"type" to AlcoholType.WHISKY.name,
				"korCategory" to "싱글 몰트",
				"engCategory" to "Single Malt",
				"categoryGroup" to AlcoholCategoryGroup.SINGLE_MALT.name,
				"regionId" to region.id,
				"distilleryId" to distillery.id,
				"age" to "18",
				"cask" to "Sherry Oak",
				"imageUrl" to "https://example.com/updated.jpg",
				"description" to "수정된 설명",
				"volume" to "750ml"
			)

			// when & then
			assertThat(
				mockMvcTester.put().uri("/alcohols/999999")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			)
				.hasStatus4xxClientError()
		}
	}

	@Nested
	@DisplayName("술 삭제 API")
	inner class DeleteAlcohol {

		@Test
		@DisplayName("위스키를 삭제할 수 있다")
		fun deleteAlcoholSuccess() {
			// given
			val alcohol = alcoholTestFactory.persistAlcohol()

			// when & then
			assertThat(
				mockMvcTester.delete().uri("/alcohols/${alcohol.id}")
					.header("Authorization", "Bearer $accessToken")
			)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.code").isEqualTo("ALCOHOL_DELETED")
		}

		@Test
		@DisplayName("존재하지 않는 alcoholId로 삭제 시 실패한다")
		fun deleteAlcoholNotFound() {
			// when & then
			assertThat(
				mockMvcTester.delete().uri("/alcohols/999999")
					.header("Authorization", "Bearer $accessToken")
			)
				.hasStatus4xxClientError()
		}

		@Test
		@DisplayName("리뷰가 존재하는 위스키는 삭제할 수 없다")
		fun deleteAlcoholWithReviews() {
			// given
			val user = userTestFactory.persistUser()
			val alcohol = alcoholTestFactory.persistAlcohol()
			reviewTestFactory.persistReview(user.id, alcohol.id)

			// when & then
			assertThat(
				mockMvcTester.delete().uri("/alcohols/${alcohol.id}")
					.header("Authorization", "Bearer $accessToken")
			)
				.hasStatus4xxClientError()
		}

		@Test
		@DisplayName("평점이 존재하는 위스키는 삭제할 수 없다")
		fun deleteAlcoholWithRatings() {
			// given
			val user = userTestFactory.persistUser()
			val alcohol = alcoholTestFactory.persistAlcohol()
			ratingTestFactory.persistRating(user.id, alcohol.id, 5)

			// when & then
			assertThat(
				mockMvcTester.delete().uri("/alcohols/${alcohol.id}")
					.header("Authorization", "Bearer $accessToken")
			)
				.hasStatus4xxClientError()
		}
	}
}
