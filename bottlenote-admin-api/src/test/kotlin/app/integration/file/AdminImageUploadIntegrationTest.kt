package app.integration.file

import app.IntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("admin_integration")
@DisplayName("[integration] Admin Image Upload API 통합 테스트")
class AdminImageUploadIntegrationTest : IntegrationTestSupport() {

	private lateinit var accessToken: String

	@BeforeEach
	fun setUp() {
		val admin = adminUserTestFactory.persistRootAdmin()
		accessToken = getAccessToken(admin)
	}

	@Nested
	@DisplayName("PreSigned URL 생성 API")
	inner class GetPreSignUrlTest {

		@Test
		@DisplayName("PreSigned URL을 생성할 수 있다")
		fun getPreSignUrl() {
			// when & then
			assertThat(
				mockMvcTester.get().uri("/s3/presign-url")
					.header("Authorization", "Bearer $accessToken")
					.param("rootPath", "admin/test")
					.param("uploadSize", "1")
			)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.success").isEqualTo(true)
		}

		@Test
		@DisplayName("여러 개의 PreSigned URL을 생성할 수 있다")
		fun getMultiplePreSignUrls() {
			// when & then
			assertThat(
				mockMvcTester.get().uri("/s3/presign-url")
					.header("Authorization", "Bearer $accessToken")
					.param("rootPath", "admin/test")
					.param("uploadSize", "3")
			)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.uploadSize").isEqualTo(3)
		}

		@Test
		@DisplayName("응답에 필요한 정보가 포함되어 있다")
		fun responseContainsRequiredFields() {
			// when
			val result = mockMvcTester.get().uri("/s3/presign-url")
				.header("Authorization", "Bearer $accessToken")
				.param("rootPath", "admin/test")
				.param("uploadSize", "2")
				.exchange()

			// then
			assertThat(result)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.bucketName").isNotNull()

			assertThat(result)
				.bodyJson()
				.extractingPath("$.data.expiryTime").isEqualTo(5)

			assertThat(result)
				.bodyJson()
				.extractingPath("$.data.imageUploadInfo").isNotNull()
		}

		@Test
		@DisplayName("생성된 URL 정보가 올바른 형식이다")
		fun urlFormatIsCorrect() {
			// when
			val result = mockMvcTester.get().uri("/s3/presign-url")
				.header("Authorization", "Bearer $accessToken")
				.param("rootPath", "admin/test")
				.param("uploadSize", "1")
				.exchange()

			val response = parseResponse(result)
			@Suppress("UNCHECKED_CAST")
			val data = response.data as Map<String, Any>
			@Suppress("UNCHECKED_CAST")
			val imageUploadInfo = data["imageUploadInfo"] as List<Map<String, Any>>

			// then
			val firstItem = imageUploadInfo[0]
			assertThat(firstItem["order"]).isEqualTo(1)
			assertThat(firstItem["viewUrl"] as String).contains("admin/test")
			assertThat(firstItem["uploadUrl"] as String).isNotEmpty()

			log.info("viewUrl = {}", firstItem["viewUrl"])
			log.info("uploadUrl = {}", firstItem["uploadUrl"])
		}

		@Test
		@DisplayName("인증 없이 요청하면 실패한다")
		fun getPreSignUrlWithoutAuth() {
			// when & then
			assertThat(
				mockMvcTester.get().uri("/s3/presign-url")
					.param("rootPath", "admin/test")
					.param("uploadSize", "1")
			)
				.hasStatus4xxClientError()
		}
	}
}
