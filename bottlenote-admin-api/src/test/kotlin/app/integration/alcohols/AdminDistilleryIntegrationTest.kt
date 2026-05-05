package app.integration.alcohols

import app.IntegrationTestSupport
import app.bottlenote.alcohols.fixture.AlcoholTestFactory
import app.bottlenote.alcohols.fixture.DistilleryTestFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType

@Tag("admin_integration")
@DisplayName("[integration] Admin Distillery API 통합 테스트")
class AdminDistilleryIntegrationTest : IntegrationTestSupport() {

	@Autowired
	private lateinit var distilleryTestFactory: DistilleryTestFactory

	@Autowired
	private lateinit var alcoholTestFactory: AlcoholTestFactory

	private lateinit var accessToken: String

	@BeforeEach
	fun setUp() {
		val admin = adminUserTestFactory.persistRootAdmin()
		accessToken = getAccessToken(admin)
	}

	@Nested
	@DisplayName("증류소 단건 조회 API")
	inner class GetDistilleryDetail {
		@Test
		@DisplayName("존재하는 증류소를 단건 조회할 수 있다")
		fun getDetailSuccess() {
			val distillery = distilleryTestFactory.persistDistillery("맥캘란", "Macallan")

			assertThat(
				mockMvcTester
					.get()
					.uri("/distilleries/${distillery.id}")
					.header("Authorization", "Bearer $accessToken")
			).hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.korName")
				.isEqualTo("맥캘란")
		}

		@Test
		@DisplayName("존재하지 않는 증류소 조회 시 404를 반환한다")
		fun getDetailNotFound() {
			assertThat(
				mockMvcTester
					.get()
					.uri("/distilleries/999999")
					.header("Authorization", "Bearer $accessToken")
			).hasStatus4xxClientError()
				.bodyJson()
				.extractingPath("$.errors[0].code")
				.isEqualTo("DISTILLERY_NOT_FOUND")
		}
	}

	@Nested
	@DisplayName("증류소 생성 API")
	inner class CreateDistillery {
		@Test
		@DisplayName("증류소를 생성할 수 있다")
		fun createSuccess() {
			val request = mapOf(
				"korName" to "토버모리",
				"engName" to "Tobermory",
				"logoImgUrl" to null
			)

			assertThat(
				mockMvcTester
					.post()
					.uri("/distilleries")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			).hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.code")
				.isEqualTo("DISTILLERY_CREATED")
		}

		@Test
		@DisplayName("중복된 한글 이름으로 생성 시 409를 반환한다")
		fun createDuplicateKorName() {
			distilleryTestFactory.persistDistillery("중복증류소", "Duplicate")
			val request = mapOf(
				"korName" to "중복증류소",
				"engName" to "Different"
			)

			assertThat(
				mockMvcTester
					.post()
					.uri("/distilleries")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			).hasStatus4xxClientError()
				.bodyJson()
				.extractingPath("$.errors[0].code")
				.isEqualTo("DISTILLERY_DUPLICATE_NAME")
		}

		@Test
		@DisplayName("빈 korName으로 생성 시 400 검증 에러를 반환한다")
		fun createBlankKorName() {
			val request = mapOf(
				"korName" to "",
				"engName" to "OnlyEng"
			)

			assertThat(
				mockMvcTester
					.post()
					.uri("/distilleries")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			).hasStatus4xxClientError()
				.bodyJson()
				.extractingPath("$.errors[0].code")
				.isEqualTo("DISTILLERY_KOR_NAME_REQUIRED")
		}
	}

	@Nested
	@DisplayName("증류소 수정 API")
	inner class UpdateDistillery {
		@Test
		@DisplayName("증류소를 수정할 수 있다")
		fun updateSuccess() {
			val distillery = distilleryTestFactory.persistDistillery("발베니", "Balvenie")
			val request = mapOf(
				"korName" to "발베니 12년",
				"engName" to "Balvenie 12"
			)

			assertThat(
				mockMvcTester
					.put()
					.uri("/distilleries/${distillery.id}")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			).hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.code")
				.isEqualTo("DISTILLERY_UPDATED")
		}

		@Test
		@DisplayName("존재하지 않는 증류소 수정 시 404를 반환한다")
		fun updateNotFound() {
			val request = mapOf(
				"korName" to "X",
				"engName" to "Y"
			)

			assertThat(
				mockMvcTester
					.put()
					.uri("/distilleries/999999")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			).hasStatus4xxClientError()
				.bodyJson()
				.extractingPath("$.errors[0].code")
				.isEqualTo("DISTILLERY_NOT_FOUND")
		}
	}

	@Nested
	@DisplayName("증류소 삭제 API")
	inner class DeleteDistillery {
		@Test
		@DisplayName("연관 위스키가 없는 증류소를 삭제할 수 있다")
		fun deleteSuccess() {
			val distillery = distilleryTestFactory.persistDistillery("아드벡", "Ardbeg")

			assertThat(
				mockMvcTester
					.delete()
					.uri("/distilleries/${distillery.id}")
					.header("Authorization", "Bearer $accessToken")
			).hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.code")
				.isEqualTo("DISTILLERY_DELETED")
		}

		@Test
		@DisplayName("존재하지 않는 증류소 삭제 시 404를 반환한다")
		fun deleteNotFound() {
			assertThat(
				mockMvcTester
					.delete()
					.uri("/distilleries/999999")
					.header("Authorization", "Bearer $accessToken")
			).hasStatus4xxClientError()
				.bodyJson()
				.extractingPath("$.errors[0].code")
				.isEqualTo("DISTILLERY_NOT_FOUND")
		}
	}
}
