package app.integration.alcohols

import app.IntegrationTestSupport
import app.bottlenote.alcohols.constant.AdminAlcoholSortType
import app.bottlenote.alcohols.constant.AlcoholCategoryGroup
import app.bottlenote.alcohols.fixture.AlcoholTestFactory
import app.bottlenote.global.service.cursor.SortOrder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import java.util.stream.Stream

@Tag("integration")
@DisplayName("[integration] Admin Alcohol API 통합 테스트")
class AdminAlcoholsIntegrationTest : IntegrationTestSupport() {

	@Autowired
	private lateinit var alcoholTestFactory: AlcoholTestFactory

	companion object {
		@JvmStatic
		fun keywordSearchTestCases(): Stream<Arguments> = Stream.of(
			Arguments.of("글렌피딕", 1, "한글 이름으로 검색"),
			Arguments.of("Glenfiddich", 1, "영문 이름으로 검색"),
			Arguments.of("위스키", 0, "매칭되지 않는 키워드")
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
		assertThat(mockMvcTester.get().uri("/alcohols"))
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
				.param("page", page.toString())
				.param("size", size.toString())
		)
			.hasStatusOk()
			.bodyJson()
			.extractingPath("$.meta.size").isEqualTo(size)
	}
}
