package app.integration.curation

import app.IntegrationTestSupport
import app.bottlenote.curation.domain.CurationSpecRepository
import app.bottlenote.curation.service.CurationSpecResourceSyncService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType

@Tag("admin_integration")
@DisplayName("[integration] Admin Spec Based Curation API 통합 테스트")
class AdminSpecBasedCurationIntegrationTest : IntegrationTestSupport() {
	@Autowired
	private lateinit var curationSpecResourceSyncService: CurationSpecResourceSyncService

	@Autowired
	private lateinit var curationSpecRepository: CurationSpecRepository

	private lateinit var accessToken: String

	@BeforeEach
	fun setUp() {
		val admin = adminUserTestFactory.persistRootAdmin()
		accessToken = getAccessToken(admin)
		curationSpecResourceSyncService.sync()
	}

	@Nested
	@DisplayName("큐레이션 스펙 조회 API")
	inner class CurationSpecs {
		@Test
		@DisplayName("활성 큐레이션 스펙 목록을 조회할 수 있다")
		fun listSpecsSuccess() {
			val result = mockMvcTester
				.get()
				.uri("/curation-specs")
				.header("Authorization", "Bearer $accessToken")
				.exchange()

			assertThat(result).hasStatusOk()
			assertThat(dataNode(result).map { it.path("code").asText() })
				.contains("RECOMMENDED_WHISKY")
		}

		@Test
		@DisplayName("큐레이션 스펙 상세에서 requestSpec과 responseSpec을 조회할 수 있다")
		fun getSpecDetailSuccess() {
			val specId = recommendedSpecId()

			assertThat(
				mockMvcTester
					.get()
					.uri("/curation-specs/$specId")
					.header("Authorization", "Bearer $accessToken")
			).hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.responseSpec.properties.stats.x-graphql.query")
				.isEqualTo("alcohols")
		}
	}

	@Nested
	@DisplayName("spec 기반 큐레이션 생성 API")
	inner class CreateSpecBasedCuration {
		@Test
		@DisplayName("request spec의 maxItems 경계값 이내 payload는 생성할 수 있다")
		fun create_whenPayloadMatchesRequestSpec_returnsCreated() {
			val request = createRequest(validPayload(selectedTags = List(12) { index -> "태그$index" }))

			assertThat(
				mockMvcTester
					.post()
					.uri("/spec-based-curations")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			).hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.code")
				.isEqualTo("CURATION_CREATED")
		}

		@Test
		@DisplayName("request spec의 required 필드가 없으면 400을 반환한다")
		fun create_whenRequiredFieldMissing_returnsBadRequest() {
			val payload = mapOf(
				"alcohol" to mapOf(
					"korName" to "필수값 테스트",
					"selectedTags" to listOf("오크")
				)
			)

			assertBadRequest(createRequest(payload))
		}

		@Test
		@DisplayName("request spec의 enum 값이 아니면 400을 반환한다")
		fun create_whenEnumValueInvalid_returnsBadRequest() {
			val payload = validPayload(source = "UNKNOWN_SOURCE")

			assertBadRequest(createRequest(payload))
		}

		@Test
		@DisplayName("request spec의 maxItems를 초과하면 400을 반환한다")
		fun create_whenSelectedTagsExceedsMaxItems_returnsBadRequest() {
			val payload = validPayload(selectedTags = List(13) { index -> "태그$index" })

			assertBadRequest(createRequest(payload))
		}

		@Test
		@DisplayName("request spec의 minLength보다 짧은 배열 항목이면 400을 반환한다")
		fun create_whenSelectedTagBelowMinLength_returnsBadRequest() {
			val payload = validPayload(selectedTags = listOf(""))

			assertBadRequest(createRequest(payload))
		}
	}

	private fun assertBadRequest(request: Map<String, Any?>) {
		assertThat(
			mockMvcTester
				.post()
				.uri("/spec-based-curations")
				.header("Authorization", "Bearer $accessToken")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(request))
		).hasStatus4xxClientError()
			.bodyJson()
			.extractingPath("$.code")
			.isEqualTo(400)
	}

	private fun createRequest(payload: Any): Map<String, Any?> = mapOf(
		"specId" to recommendedSpecId(),
		"name" to "통합 테스트 큐레이션",
		"description" to "request spec 검증 테스트",
		"imageUrls" to listOf("https://cdn.example.com/cover.jpg"),
		"exposureStartDate" to "2026-06-01",
		"exposureEndDate" to "2026-06-30",
		"displayOrder" to 1,
		"isActive" to true,
		"payload" to listOf(payload)
	)

	private fun validPayload(
		source: String = "BOTTLE_NOTE",
		selectedTags: List<String> = listOf("셰리", "오크")
	): Map<String, Any?> = mapOf(
		"source" to source,
		"alcohol" to mapOf(
			"alcoholId" to 1L,
			"korName" to "검증 위스키",
			"selectedTags" to selectedTags
		),
		"comment" to "테스트 코멘트"
	)

	private fun dataNode(result: org.springframework.test.web.servlet.assertj.MvcTestResult) = mapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(parseResponse(result).data)

	private fun recommendedSpecId(): Long = curationSpecRepository.findByCode("RECOMMENDED_WHISKY").orElseThrow().id
}
