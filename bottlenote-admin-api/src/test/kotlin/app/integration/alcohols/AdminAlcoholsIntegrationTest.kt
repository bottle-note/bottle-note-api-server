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
import org.springframework.beans.factory.annotation.Autowired

@Tag("integration")
@DisplayName("[integration] Admin Alcohol API 통합 테스트")
class AdminAlcoholsIntegrationTest : IntegrationTestSupport() {

	@Autowired
	private lateinit var alcoholTestFactory: AlcoholTestFactory

	@Test
	@DisplayName("관리자용 술 목록을 조회할 수 있다")
	fun searchAdminAlcohols() {
		// given
		alcoholTestFactory.persistAlcohols(5)

		// when & then
		assertThat(
			mockMvcTester.get().uri("/alcohols")
		)
			.hasStatusOk()
			.bodyJson()
			.extractingPath("$.success").isEqualTo(true)
	}

	@Test
	@DisplayName("키워드로 술을 검색할 수 있다")
	fun searchByKeyword() {
		// given
		alcoholTestFactory.persistAlcoholWithName("글렌피딕 12년", "Glenfiddich 12")
		alcoholTestFactory.persistAlcoholWithName("맥캘란 18년", "Macallan 18")

		// when & then
		assertThat(
			mockMvcTester.get().uri("/alcohols")
				.param("keyword", "글렌피딕")
		)
			.hasStatusOk()
			.bodyJson()
			.extractingPath("$.data.length()").isEqualTo(1)
	}

	@Test
	@DisplayName("카테고리로 필터링할 수 있다")
	fun filterByCategory() {
		// given
		alcoholTestFactory.persistAlcohols(3)

		// when & then
		assertThat(
			mockMvcTester.get().uri("/alcohols")
				.param("category", AlcoholCategoryGroup.SINGLE_MALT.name)
		)
			.hasStatusOk()
			.bodyJson()
			.extractingPath("$.success").isEqualTo(true)
	}

	@Test
	@DisplayName("한글 이름으로 정렬할 수 있다")
	fun sortByKorName() {
		// given
		alcoholTestFactory.persistAlcoholWithName("가나다 위스키", "ABC Whisky")
		alcoholTestFactory.persistAlcoholWithName("마바사 위스키", "DEF Whisky")

		// when & then
		assertThat(
			mockMvcTester.get().uri("/alcohols")
				.param("sortType", AdminAlcoholSortType.KOR_NAME.name)
				.param("sortOrder", SortOrder.ASC.name)
		)
			.hasStatusOk()
			.bodyJson()
			.extractingPath("$.success").isEqualTo(true)
	}

	@Test

	@DisplayName("페이징이 정상 동작한다")
	fun pagination() {
		// given
		alcoholTestFactory.persistAlcohols(25)

		// when & then
		assertThat(
			mockMvcTester.get().uri("/alcohols")
				.param("page", "0")
				.param("size", "10")
		)
			.hasStatusOk()
			.bodyJson()
			.extractingPath("$.meta.size").isEqualTo(10)
	}
}
