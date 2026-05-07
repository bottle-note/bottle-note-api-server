package app.integration.region

import app.IntegrationTestSupport
import app.bottlenote.alcohols.dto.request.AdminRegionCreateRequest
import app.bottlenote.alcohols.dto.request.AdminRegionSortOrderRequest
import app.bottlenote.alcohols.dto.request.AdminRegionUpdateRequest
import app.bottlenote.alcohols.fixture.RegionTestFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType

@Tag("admin_integration")
@DisplayName("[integration] Admin Region API 통합 테스트")
class AdminRegionIntegrationTest : IntegrationTestSupport() {

	@Autowired
	private lateinit var regionTestFactory: RegionTestFactory

	private lateinit var accessToken: String

	@BeforeEach
	fun setUp() {
		val admin = adminUserTestFactory.persistRootAdmin()
		accessToken = getAccessToken(admin)
	}

	@Nested
	@DisplayName("지역 단건 조회 API")
	inner class Detail {
		@Test
		@DisplayName("지역 상세 정보를 조회할 수 있다")
		fun detailSuccess() {
			val region = regionTestFactory.persistRoot("스코틀랜드", "Scotland", 10)

			assertThat(
				mockMvcTester
					.get()
					.uri("/regions/${region.id}")
					.header("Authorization", "Bearer $accessToken")
			).hasStatusOk()
				.bodyJson()
				.extractingPath("$.success").isEqualTo(true)
		}

		@Test
		@DisplayName("존재하지 않는 지역 조회 시 404를 반환한다")
		fun detailNotFound() {
			assertThat(
				mockMvcTester
					.get()
					.uri("/regions/999999")
					.header("Authorization", "Bearer $accessToken")
			).hasStatus(404)
		}
	}

	@Nested
	@DisplayName("지역 생성 API")
	inner class Create {
		@Test
		@DisplayName("지역을 생성할 수 있다")
		fun createSuccess() {
			val request = AdminRegionCreateRequest.builder()
				.korName("아일랜드")
				.engName("Ireland")
				.continent("Europe")
				.description("아일리시")
				.sortOrder(20)
				.build()

			assertThat(
				mockMvcTester
					.post()
					.uri("/regions")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			).hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.code").isEqualTo("REGION_CREATED")
		}

		@Test
		@DisplayName("한글 이름이 중복되면 409를 반환한다")
		fun createDuplicateKorName() {
			regionTestFactory.persistRoot("스코틀랜드", "Scotland", 10)

			val request = AdminRegionCreateRequest.builder()
				.korName("스코틀랜드")
				.engName("ScotlandUk")
				.build()

			assertThat(
				mockMvcTester
					.post()
					.uri("/regions")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			).hasStatus(409)
		}
	}

	@Nested
	@DisplayName("지역 수정 API")
	inner class Update {
		@Test
		@DisplayName("지역을 수정할 수 있다")
		fun updateSuccess() {
			val region = regionTestFactory.persistRoot("스코트랜드", "ScotlandTypo", 10)

			val request = AdminRegionUpdateRequest.builder()
				.korName("스코틀랜드")
				.engName("Scotland")
				.continent("Europe")
				.description("정정")
				.sortOrder(10)
				.build()

			assertThat(
				mockMvcTester
					.put()
					.uri("/regions/${region.id}")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			).hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.code").isEqualTo("REGION_UPDATED")
		}
	}

	@Nested
	@DisplayName("지역 삭제 API")
	inner class Delete {
		@Test
		@DisplayName("지역을 삭제할 수 있다")
		fun deleteSuccess() {
			val region = regionTestFactory.persistRoot("스코틀랜드", "Scotland", 10)

			assertThat(
				mockMvcTester
					.delete()
					.uri("/regions/${region.id}")
					.header("Authorization", "Bearer $accessToken")
			).hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.code").isEqualTo("REGION_DELETED")
		}

		@Test
		@DisplayName("자식이 있는 지역은 삭제할 수 없다")
		fun deleteHasChildren() {
			val root = regionTestFactory.persistRoot("스코틀랜드", "Scotland", 10)
			regionTestFactory.persistRegion("하이랜드", "Highland", root, 20)

			assertThat(
				mockMvcTester
					.delete()
					.uri("/regions/${root.id}")
					.header("Authorization", "Bearer $accessToken")
			).hasStatus(409)
		}
	}

	@Nested
	@DisplayName("지역 정렬 변경 API")
	inner class UpdateSortOrder {
		@Test
		@DisplayName("정렬 순서를 변경할 수 있다")
		fun sortOrderSuccess() {
			val region = regionTestFactory.persistRoot("스코틀랜드", "Scotland", 100)

			val request = AdminRegionSortOrderRequest(50)

			assertThat(
				mockMvcTester
					.patch()
					.uri("/regions/${region.id}/sort-order")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			).hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.code").isEqualTo("REGION_SORT_ORDER_UPDATED")
		}
	}
}
