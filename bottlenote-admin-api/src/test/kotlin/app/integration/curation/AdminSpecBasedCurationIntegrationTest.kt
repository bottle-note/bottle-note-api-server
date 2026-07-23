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
import java.time.LocalDate

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
				.uri("/v2/curation-specs")
				.header("Authorization", "Bearer $accessToken")
				.exchange()

			assertThat(result).hasStatusOk()
			val data = dataNode(result)
			assertThat(data.map { it.path("code").asText() })
				.contains("RECOMMENDED_WHISKY")
			assertThat(data.first().has("requestSpec")).isFalse()
			assertThat(data.first().has("responseSpec")).isFalse()
			assertThat(data.first().has("hydratorKey")).isFalse()
		}

		@Test
		@DisplayName("Admin v2에서 인증 없이 /v2/curation-specs를 요청할 경우 4xx를 반환한다")
		fun listSpecs_whenUnauthenticated_returnsUnauthorized() {
			assertThat(
				mockMvcTester
					.get()
					.uri("/v2/curation-specs")
			).hasStatus4xxClientError()
		}

		@Test
		@DisplayName("큐레이션 스펙 상세에서 requestSpec과 responseSpec을 조회할 수 있다")
		fun getSpecDetailSuccess() {
			val specId = recommendedSpecId()

			assertThat(
				mockMvcTester
					.get()
					.uri("/v2/curation-specs/$specId")
					.header("Authorization", "Bearer $accessToken")
			).hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.responseSpec.properties.stats.x-graphql.query")
				.isEqualTo("alcohols")

			assertThat(
				mockMvcTester
					.get()
					.uri("/v2/curation-specs/$specId")
					.header("Authorization", "Bearer $accessToken")
			).hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.hydratorKey")
				.isEqualTo("alcohol")
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
					.uri("/v2/curations")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			).hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.code")
				.isEqualTo("CURATION_CREATED")
		}

		@Test
		@DisplayName("Admin v2에서 존재하지 않는 specId로 생성할 경우 404를 반환한다")
		fun create_whenSpecIdDoesNotExist_returnsNotFound() {
			val request = createRequest(validPayload()) + ("specId" to 999999L)

			assertNotFound(
				mockMvcTester
					.post()
					.uri("/v2/curations")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
					.exchange()
			)
		}

		@Test
		@DisplayName("Admin v2에서 인증 없이 /v2/curations를 요청할 경우 4xx를 반환한다")
		fun listCurations_whenUnauthenticated_returnsUnauthorized() {
			assertThat(
				mockMvcTester
					.get()
					.uri("/v2/curations")
			).hasStatus4xxClientError()
		}

		@Test
		@DisplayName("Admin 큐레이션 목록은 code, keyword, isActive 조건을 조합해 조회할 수 있다")
		fun listCurations_whenCodeKeywordAndActiveProvided_filtersResults() {
			mockMvcTester
				.post()
				.uri("/v2/curations")
				.header("Authorization", "Bearer $accessToken")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(createRequest(validPayload(), name = "통합 매치 큐레이션", isActive = true)))
				.exchange()

			val result = mockMvcTester
				.get()
				.uri("/v2/curations?keyword=매치&code=RECOMMENDED_WHISKY&isActive=true&page=0&size=20")
				.header("Authorization", "Bearer $accessToken")
				.exchange()

			assertThat(result).hasStatusOk()
			val data = dataNode(result)
			assertThat(data.map { it.path("name").asText() }).contains("통합 매치 큐레이션")
			assertThat(data.map { it.path("specCode").asText() }.distinct()).containsOnly("RECOMMENDED_WHISKY")
		}

		@Test
		@DisplayName("Admin 큐레이션 목록은 알 수 없는 code를 빈 결과로 반환한다")
		fun listCurations_whenUnknownCodeProvided_returnsEmptyResult() {
			mockMvcTester
				.post()
				.uri("/v2/curations")
				.header("Authorization", "Bearer $accessToken")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(createRequest(validPayload(), name = "통합 네거티브 큐레이션")))
				.exchange()

			val result = mockMvcTester
				.get()
				.uri("/v2/curations?code=UNKNOWN_CODE&page=0&size=20")
				.header("Authorization", "Bearer $accessToken")
				.exchange()

			assertThat(result).hasStatusOk()
			assertThat(dataNode(result)).isEmpty()
		}

		@Test
		@DisplayName("Admin 큐레이션 목록은 blank code를 필터 미적용으로 처리한다")
		fun listCurations_whenBlankCodeProvided_ignoresCodeFilter() {
			mockMvcTester
				.post()
				.uri("/v2/curations")
				.header("Authorization", "Bearer $accessToken")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(createRequest(validPayload(), name = "boundary-filter-curation")))
				.exchange()

			val result = mockMvcTester
				.get()
				.uri("/v2/curations?keyword=boundary&code=&page=0&size=20")
				.header("Authorization", "Bearer $accessToken")
				.exchange()

			assertThat(result).hasStatusOk()
			assertThat(dataNode(result).map { it.path("name").asText() }).contains("boundary-filter-curation")
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
		@DisplayName("요청 DTO 필수 필드가 없으면 400을 반환한다")
		fun create_whenRequestRequiredFieldMissing_returnsBadRequest() {
			val request = createRequest(validPayload()) - "name"

			assertBadRequest(request)
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

	@Nested
	@DisplayName("spec 기반 큐레이션 수정 API")
	inner class UpdateSpecBasedCuration {
		@Test
		@DisplayName("Admin v2에서 존재하지 않는 curationId로 수정할 경우 404를 반환한다")
		fun update_whenCurationIdDoesNotExist_returnsNotFound() {
			assertNotFound(
				mockMvcTester
					.put()
					.uri("/v2/curations/{curationId}", 999999L)
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(updateRequest(validPayload())))
					.exchange()
			)
		}
	}

	private fun assertBadRequest(request: Map<String, Any?>) {
		assertThat(
			mockMvcTester
				.post()
				.uri("/v2/curations")
				.header("Authorization", "Bearer $accessToken")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(request))
		).hasStatus4xxClientError()
			.bodyJson()
			.extractingPath("$.code")
			.isEqualTo(400)
	}

	private fun assertNotFound(result: org.springframework.test.web.servlet.assertj.MvcTestResult) {
		assertThat(result).hasStatus(404)
			.bodyJson()
			.extractingPath("$.code")
			.isEqualTo(404)
	}

	private fun createRequest(
		payload: Any,
		name: String = "통합 테스트 큐레이션",
		isActive: Boolean = true
	): Map<String, Any?> = mapOf(
		"specId" to recommendedSpecId(),
		"name" to name,
		"description" to "request spec 검증 테스트",
		"imageUrls" to listOf("https://cdn.example.com/cover.jpg"),
		"exposureStartDate" to LocalDate.now().minusDays(1).toString(),
		"exposureEndDate" to LocalDate.now().plusDays(30).toString(),
		"displayOrder" to 1,
		"isActive" to isActive,
		"payload" to listOf(payload)
	)

	private fun updateRequest(payload: Any): Map<String, Any?> = mapOf(
		"specId" to recommendedSpecId(),
		"name" to "수정 테스트 큐레이션",
		"description" to "존재하지 않는 큐레이션 수정 테스트",
		"imageUrls" to listOf("https://cdn.example.com/cover.jpg"),
		"exposureStartDate" to LocalDate.now().minusDays(1).toString(),
		"exposureEndDate" to LocalDate.now().plusDays(30).toString(),
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
