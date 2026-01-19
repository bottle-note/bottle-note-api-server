package app.integration.alcohols

import app.IntegrationTestSupport
import app.bottlenote.alcohols.fixture.AlcoholTestFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@Tag("admin_integration")
@DisplayName("[integration] Admin 참조 데이터 API 통합 테스트")
class AdminReferenceDataIntegrationTest : IntegrationTestSupport() {

	@Autowired
	private lateinit var alcoholTestFactory: AlcoholTestFactory

	private lateinit var accessToken: String

	@BeforeEach
	fun setUp() {
		val admin = adminUserTestFactory.persistRootAdmin()
		accessToken = getAccessToken(admin)
	}

	@Nested
	@DisplayName("테이스팅 태그 목록 조회 API")
	inner class TastingTagsApi {

		@Test
		@DisplayName("전체 테이스팅 태그 목록을 페이지네이션으로 조회할 수 있다")
		fun getAllTastingTagsSuccess() {
			// when & then
			assertThat(
				mockMvcTester.get().uri("/tasting-tags?page=0&size=20")
					.header("Authorization", "Bearer $accessToken")
			)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.success").isEqualTo(true)

			// 페이지네이션 메타 정보 확인
			assertThat(
				mockMvcTester.get().uri("/tasting-tags?page=0&size=10")
					.header("Authorization", "Bearer $accessToken")
			)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.meta.size").isEqualTo(10)
		}

		@Test
		@DisplayName("인증 없이 요청 시 실패한다")
		fun getTastingTagsWithoutAuth() {
			// when & then - 방어로직: 인증 없이 요청 시 실패
			assertThat(
				mockMvcTester.get().uri("/tasting-tags")
			)
				.hasStatus4xxClientError()
		}
	}

	@Nested
	@DisplayName("지역 목록 조회 API")
	inner class RegionsApi {

		@Test
		@DisplayName("전체 지역 목록을 페이지네이션으로 조회할 수 있다")
		fun getAllRegionsSuccess() {
			// given - alcoholTestFactory에서 region 데이터가 함께 생성됨
			alcoholTestFactory.persistAlcohols(1)

			// when & then
			assertThat(
				mockMvcTester.get().uri("/regions?page=0&size=20")
					.header("Authorization", "Bearer $accessToken")
			)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.success").isEqualTo(true)
		}

		@Test
		@DisplayName("지역 목록이 페이지네이션 메타 정보를 포함한다")
		fun getRegionsWithPaginationMeta() {
			// given
			alcoholTestFactory.persistAlcohols(1)

			// when & then - 응답 데이터 및 페이지네이션 메타 확인
			val result = mockMvcTester.get().uri("/regions?page=0&size=10")
				.header("Authorization", "Bearer $accessToken")

			assertThat(result)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.meta.page").isEqualTo(0)

			assertThat(result)
				.bodyJson()
				.extractingPath("$.meta.totalElements").isNotNull

			// 방어로직: 인증 없이 요청 시 실패
			assertThat(
				mockMvcTester.get().uri("/regions")
			)
				.hasStatus4xxClientError()
		}
	}

	@Nested
	@DisplayName("증류소 목록 조회 API")
	inner class DistilleriesApi {

		@Test
		@DisplayName("전체 증류소 목록을 페이지네이션으로 조회할 수 있다")
		fun getAllDistilleriesSuccess() {
			// given - alcoholTestFactory에서 distillery 데이터가 함께 생성됨
			alcoholTestFactory.persistAlcohols(1)

			// when & then
			assertThat(
				mockMvcTester.get().uri("/distilleries?page=0&size=20")
					.header("Authorization", "Bearer $accessToken")
			)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.success").isEqualTo(true)
		}

		@Test
		@DisplayName("증류소 목록을 키워드로 검색할 수 있다")
		fun getDistilleriesWithKeyword() {
			// given
			alcoholTestFactory.persistAlcohols(1)

			// when & then - 키워드 검색
			val result = mockMvcTester.get().uri("/distilleries?keyword=&page=0&size=20")
				.header("Authorization", "Bearer $accessToken")

			assertThat(result)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.data").isNotNull

			// 방어로직: 인증 없이 요청 시 실패
			assertThat(
				mockMvcTester.get().uri("/distilleries")
			)
				.hasStatus4xxClientError()
		}
	}
}
