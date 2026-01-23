package app.integration.file

import app.IntegrationTestSupport
import app.bottlenote.operation.utils.TestContainersConfig
import com.amazonaws.services.s3.AmazonS3
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URI

@Tag("admin_integration")
@DisplayName("[integration] Admin Image Upload API 통합 테스트")
class AdminImageUploadIntegrationTest : IntegrationTestSupport() {

	@Autowired
	private lateinit var amazonS3: AmazonS3

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

	@Nested
	@DisplayName("PreSigned URL 업로드 시나리오")
	inner class UploadScenarioTest {

		@Test
		@DisplayName("PreSigned URL로 파일을 업로드하고 S3에서 확인할 수 있다")
		fun uploadAndVerifyFile() {
			// given: PreSigned URL 발급
			val result = mockMvcTester.get().uri("/s3/presign-url")
				.header("Authorization", "Bearer $accessToken")
				.param("rootPath", "admin/upload-test")
				.param("uploadSize", "1")
				.exchange()

			val response = parseResponse(result)
			@Suppress("UNCHECKED_CAST")
			val data = response.data as Map<String, Any>
			@Suppress("UNCHECKED_CAST")
			val imageUploadInfo = data["imageUploadInfo"] as List<Map<String, Any>>
			val uploadUrl = imageUploadInfo[0]["uploadUrl"] as String
			val viewUrl = imageUploadInfo[0]["viewUrl"] as String

			// viewUrl에서 S3 key 추출 (cloudFrontUrl 이후 부분)
			val s3Key = viewUrl.substringAfter("fake-cloudfront.net/")

			log.info("uploadUrl = {}", uploadUrl)
			log.info("s3Key = {}", s3Key)

			// when: PreSigned URL로 파일 업로드
			val testContent = "test image content"
			val responseCode = uploadToPreSignedUrl(uploadUrl, testContent)

			// then: 업로드 성공 확인
			assertThat(responseCode).isEqualTo(200)

			// then: S3에서 파일 존재 확인
			val bucketName = TestContainersConfig.getTestBucket()
			val exists = amazonS3.doesObjectExist(bucketName, s3Key)
			assertThat(exists).isEqualTo(true)

			// then: 업로드된 내용 확인
			val s3Object = amazonS3.getObject(bucketName, s3Key)
			val content = s3Object.objectContent.bufferedReader().use { it.readText() }
			assertThat(content).isEqualTo(testContent)

			log.info("업로드된 파일 내용 확인 완료: {}", content)
		}

		@Test
		@DisplayName("여러 파일을 업로드하고 모두 S3에서 확인할 수 있다")
		fun uploadMultipleFilesAndVerify() {
			// given: PreSigned URL 3개 발급
			val result = mockMvcTester.get().uri("/s3/presign-url")
				.header("Authorization", "Bearer $accessToken")
				.param("rootPath", "admin/multi-upload")
				.param("uploadSize", "3")
				.exchange()

			val response = parseResponse(result)
			@Suppress("UNCHECKED_CAST")
			val data = response.data as Map<String, Any>
			@Suppress("UNCHECKED_CAST")
			val imageUploadInfo = data["imageUploadInfo"] as List<Map<String, Any>>

			// when: 3개 파일 모두 업로드
			val uploadResults = imageUploadInfo.mapIndexed { index, info ->
				val uploadUrl = info["uploadUrl"] as String
				val viewUrl = info["viewUrl"] as String
				val s3Key = viewUrl.substringAfter("fake-cloudfront.net/")
				val content = "content-$index"
				val responseCode = uploadToPreSignedUrl(uploadUrl, content)
				Triple(s3Key, content, responseCode)
			}

			// then: 모든 업로드 성공 확인
			val bucketName = TestContainersConfig.getTestBucket()
			uploadResults.forEach { (s3Key, expectedContent, responseCode) ->
				assertThat(responseCode).isEqualTo(200)
				assertThat(amazonS3.doesObjectExist(bucketName, s3Key)).isEqualTo(true)

				val actualContent = amazonS3.getObject(bucketName, s3Key)
					.objectContent.bufferedReader().use { it.readText() }
				assertThat(actualContent).isEqualTo(expectedContent)
			}

			log.info("3개 파일 업로드 및 검증 완료")
		}

		private fun uploadToPreSignedUrl(preSignedUrl: String, content: String): Int {
			val url = URI(preSignedUrl).toURL()
			val connection = url.openConnection() as HttpURLConnection
			return try {
				connection.doOutput = true
				connection.requestMethod = "PUT"
				connection.setRequestProperty("Content-Type", "application/octet-stream")

				OutputStreamWriter(connection.outputStream).use { writer ->
					writer.write(content)
				}

				connection.responseCode
			} finally {
				connection.disconnect()
			}
		}
	}
}
