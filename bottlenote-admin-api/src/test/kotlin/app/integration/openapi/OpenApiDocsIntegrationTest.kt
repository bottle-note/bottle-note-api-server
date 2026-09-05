package app.integration.openapi

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("admin_integration")
@DisplayName("[integration] Admin OpenAPI 스펙 문서 노출")
class OpenApiDocsIntegrationTest : OpenApiSpecTestSupport() {

	private val envelopeFields = listOf("success", "code", "data", "errors", "meta")

	// Admin의 86 operation은 대부분 GlobalResponse 공통 형식을 쓴다.
	// 템플릿 다운로드(GET /v1/alcohols/excel/template)는 XLSX binary 응답이라 예외다.
	// 기존 84 + bulk validate/create 2 = 86
	private val expectedOperationCount = 86
	private val binaryDownloadOperations = setOf("GET /v1/alcohols/excel/template")

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
	@DisplayName("문서에는 86개 operation이 누락 없이 포함된다")
	fun openApiSpecContains86Operations() {
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
		val operations = operationsOf(fetchSpec()).filterNot { it.endpoint() in binaryDownloadOperations }

		assertThat(operations).isNotEmpty()
		assertThat(operations)
			.allSatisfy { operation ->
				assertThat(propertyNamesOf(operation.successSchema()))
					.withFailMessage("%s 의 성공 응답이 공통 형식이 아닙니다", operation)
					.containsExactlyInAnyOrderElementsOf(envelopeFields)
			}
	}

	@Test
	@DisplayName("주류 등록 요청에서 imageUrl은 선택 필드로 문서화된다")
	fun alcoholCreateDocumentsImageUrlAsOptional() {
		val spec = fetchSpec()
		val operation = operationsOf(spec).first { it.endpoint() == "POST /v1/alcohols" }
		val schemaRef = operation.definition.at("/requestBody/content/application~1json/schema/\$ref").asText()
		val requestSchema = spec.at(schemaRef.removePrefix("#"))

		assertThat(propertyNamesOf(requestSchema)).contains("imageUrl")
		assertThat(requestSchema.path("required").map { it.asText() }).doesNotContain("imageUrl")
	}

	@Test
	@DisplayName("수입 신고 목록은 상세와 동일한 nullable SKU 표시명 필드를 문서화한다")
	fun mfdsDeclarationListDocumentsSkuDisplayNames() {
		val spec = fetchSpec()
		val listSchema = spec.at("/components/schemas/MfdsDeclarationListItem")
		val detailSchema = spec.at("/components/schemas/MfdsDeclarationDetailResponse")

		assertThat(propertyNamesOf(listSchema)).contains("skuDisplayNameKo", "skuDisplayNameEn")
		assertThat(listSchema.at("/properties/skuDisplayNameKo"))
			.isEqualTo(detailSchema.at("/properties/skuDisplayNameKo"))
		assertThat(listSchema.at("/properties/skuDisplayNameEn"))
			.isEqualTo(detailSchema.at("/properties/skuDisplayNameEn"))
		assertThat(listSchema.path("required").map { it.asText() })
			.doesNotContain("skuDisplayNameKo", "skuDisplayNameEn")
	}

	@Test
	@DisplayName("공통 형식의 data는 이중으로 감싸지지 않는다")
	fun globalEnvelopeIsNotNestedInData() {
		val operations = operationsOf(fetchSpec()).filterNot { it.endpoint() in binaryDownloadOperations }

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

	@Test
	@DisplayName("엑셀 템플릿 다운로드는 XLSX binary 응답으로 문서화된다")
	fun excelTemplateDownloadIsDocumentedAsBinary() {
		val operation =
			operationsOf(fetchSpec()).first { it.endpoint() == "GET /v1/alcohols/excel/template" }

		val content = operation.definition.at("/responses/200/content")
		assertThat(content.has(ALCOHOL_EXCEL_CONTENT_TYPE)).isTrue()
	}

	@Test
	@DisplayName("엑셀 검증 업로드 엔드포인트가 문서화된다")
	fun excelValidateEndpointIsDocumented() {
		val operation =
			operationsOf(fetchSpec()).first { it.endpoint() == "POST /v1/alcohols/excel/validate" }

		assertThat(operation.summary()).isNotBlank()
		assertThat(propertyNamesOf(operation.successSchema()))
			.containsExactlyInAnyOrderElementsOf(envelopeFields)
	}

	companion object {
		private const val ALCOHOL_EXCEL_CONTENT_TYPE =
			"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
	}
}
