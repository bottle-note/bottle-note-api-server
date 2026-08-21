package app.integration.openapi

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("admin_integration")
@DisplayName("[integration] Admin OpenAPI 스펙 문서 노출")
class OpenApiDocsIntegrationTest : OpenApiSpecTestSupport() {

	private val envelopeFields = listOf("success", "code", "data", "errors", "meta")

	// Admin의 82 operation은 모두 GlobalResponse 공통 형식을 쓴다 (plan Assumption 6).
	// product와 달리 공통 형식을 벗어나는 예외 엔드포인트가 없다.
	private val expectedOperationCount = 82

	@Test
	@DisplayName("인증 없이 스펙 문서를 조회할 수 있다")
	fun openApiSpecCanBeReadWithoutAuthentication() {
		assertThat(mockMvcTester.get().uri(specPath))
			.hasStatusOk()
			.bodyJson()
			.extractingPath("$.info.title")
			.isEqualTo("보틀노트 Admin API")
	}

	@Test
	@DisplayName("스펙 문서는 OpenAPI 3.1 형식이다")
	fun openApiSpecUsesVersion31() {
		assertThat(mockMvcTester.get().uri(specPath))
			.hasStatusOk()
			.bodyJson()
			.extractingPath("$.openapi")
			.asString()
			.startsWith("3.1")
	}

	@Test
	@DisplayName("swagger-ui는 비활성화되어 접근할 수 없다")
	fun swaggerUiIsDisabled() {
		val result = mockMvcTester.get().uri("/swagger-ui/index.html").exchange()

		assertThat(result.response.status).isNotEqualTo(200)
	}

	@Test
	@DisplayName("문서에는 82개 operation이 누락 없이 포함된다")
	fun openApiSpecContains82Operations() {
		val operations = operationsOf(fetchSpec())

		assertThat(operations)
			.withFailMessage(
				"operation 수가 %d건입니다(기대: %d건):%n%s",
				operations.size,
				expectedOperationCount,
				joined(operations.map { it.toString() })
			)
			.hasSize(expectedOperationCount)
	}

	@Test
	@DisplayName("모든 엔드포인트의 성공 응답은 GlobalResponse 공통 형식이다")
	fun everySuccessResponseUsesGlobalEnvelope() {
		val operations = operationsOf(fetchSpec())

		assertThat(operations).isNotEmpty()
		assertThat(operations)
			.allSatisfy { operation ->
				assertThat(propertyNamesOf(operation.successSchema()))
					.withFailMessage("%s 의 성공 응답이 공통 형식이 아닙니다", operation)
					.containsExactlyInAnyOrderElementsOf(envelopeFields)
			}
	}

	@Test
	@DisplayName("공통 형식의 data는 이중으로 감싸지지 않는다")
	fun globalEnvelopeIsNotNestedInData() {
		val operations = operationsOf(fetchSpec())

		val doublyWrapped =
			operations
				.filter { operation ->
					val dataSchema = operation.successSchema().path("properties").path("data")
					val dataProperties = propertyNamesOf(dataSchema)
					dataSchema.path("\$ref").asText().endsWith("/GlobalResponse") ||
						(dataProperties.isNotEmpty() && dataProperties.containsAll(envelopeFields))
				}
				.map { it.toString() }

		assertThat(doublyWrapped)
			.withFailMessage("공통 형식이 data 안에 중첩된 엔드포인트가 있습니다:%n%s", joined(doublyWrapped))
			.isEmpty()
	}
}
