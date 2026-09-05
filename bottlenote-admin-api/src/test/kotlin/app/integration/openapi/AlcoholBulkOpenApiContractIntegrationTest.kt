package app.integration.openapi

import com.fasterxml.jackson.databind.JsonNode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("admin_integration")
@DisplayName("[integration] Admin Alcohol Bulk OpenAPI 계약")
class AlcoholBulkOpenApiContractIntegrationTest : OpenApiSpecTestSupport() {
	@Test
	@DisplayName("벌크 검증과 등록 엔드포인트가 v1 경로와 JSON 요청 본문으로 문서화된다")
	fun bulkEndpointsDocumentVersionedPathsAndJsonBody() {
		val spec = fetchSpec()
		val operations = bulkOperations(spec)

		assertThat(operations.map { it.endpoint() })
			.containsExactlyInAnyOrder("POST /v1/alcohols/bulk/validate", "POST /v1/alcohols/bulk")
		operations.forEach { operation ->
			val requestSchema = operation.definition.at("/requestBody/content/application~1json/schema")
			assertThat(requestSchema.path("\$ref").asText()).endsWith("/AdminAlcoholBulkRequest")
		}
		val requestSchema = spec.at("/components/schemas/AdminAlcoholBulkRequest")
		assertThat(propertyNamesOf(requestSchema)).containsExactly("rows")
		assertThat(requestSchema.at("/properties/rows/maxItems").asInt()).isEqualTo(1000)
		assertThat(requestSchema.at("/properties/rows/minItems").asInt()).isEqualTo(1)
		assertThat(requestSchema.at("/properties/rows/items/\$ref").asText()).endsWith("/AdminAlcoholBulkRowRequest")
	}

	@Test
	@DisplayName("벌크 검증과 등록은 200·400·401·403 응답과 bearerAuth 요구를 문서화한다")
	fun bulkEndpointsDocumentResponsesAndAuthentication() {
		val operations = bulkOperations(fetchSpec())

		operations.forEach { operation ->
			assertThat(childNamesOf(operation.definition.path("responses")))
				.contains("200", "400", "401", "403")
			assertThat(operation.security().any { item -> item.has("bearerAuth") }).isTrue()
		}
	}

	@Test
	@DisplayName("벌크 성공 및 검증 실패 응답이 생성 ID와 validation 구조를 노출한다")
	fun bulkResponseSchemasExposeCreatedRowsAndValidation() {
		val spec = fetchSpec()
		val validate = bulkOperations(spec).first { it.endpoint().endsWith("/validate") }
		val create = bulkOperations(spec).first { it.endpoint() == "POST /v1/alcohols/bulk" }
		val validateData = referencedSchema(spec, validate.successSchema().at("/properties/data"))
		val createData = referencedSchema(spec, create.successSchema().at("/properties/data"))

		assertThat(propertyNamesOf(validateData))
			.contains("totalRows", "validRows", "invalidRows", "warningRows", "rows")
		assertThat(propertyNamesOf(createData)).contains("createdRows", "rows", "validation")
		val createdRowSchema = referencedSchema(spec, createData.at("/properties/rows/items"))
		assertThat(propertyNamesOf(createdRowSchema)).contains("clientRowId", "alcoholId")
		val failureSchema = create.definition.at("/responses/400/content/application~1json/schema")
		val failureRefs = failureSchema.path("oneOf").map { it.path("\$ref").asText() }
		assertThat(failureRefs)
			.anyMatch { it.endsWith("/AlcoholBulkValidationFailureEnvelope") }
			.anyMatch { it.endsWith("/AlcoholBulkRequestFailureEnvelope") }
		val validationFailure = referencedSchema(
			spec,
			failureSchema.path("oneOf").first { node ->
				node.path("\$ref").asText().endsWith("/AlcoholBulkValidationFailureEnvelope")
			}
		)
		val requestFailure = referencedSchema(
			spec,
			failureSchema.path("oneOf").first { node ->
				node.path("\$ref").asText().endsWith("/AlcoholBulkRequestFailureEnvelope")
			}
		)
		val validationErrors = referencedSchema(spec, validationFailure.at("/properties/errors"))
		assertThat(propertyNamesOf(validationErrors)).contains("invalidRows", "rows")
		assertThat(requestFailure.at("/properties/errors/type").asText()).isEqualTo("array")
		assertThat(requestFailure.at("/properties/errors/items/\$ref").asText()).endsWith("/Error")
		val validateRequestFailure = validate.definition.at("/responses/400/content/application~1json/schema")
		assertThat(validateRequestFailure.path("\$ref").asText()).endsWith("/AlcoholBulkRequestFailureEnvelope")
	}

	private fun bulkOperations(spec: JsonNode): List<SpecOperation> = operationsOf(spec).filter { it.endpoint() in BULK_ENDPOINTS }

	private fun referencedSchema(
		spec: JsonNode,
		schema: JsonNode
	): JsonNode {
		val ref = schema.path("\$ref").asText()
		return if (ref.isBlank()) schema else spec.at(ref.removePrefix("#"))
	}

	companion object {
		private val BULK_ENDPOINTS =
			setOf("POST /v1/alcohols/bulk/validate", "POST /v1/alcohols/bulk")
	}
}
