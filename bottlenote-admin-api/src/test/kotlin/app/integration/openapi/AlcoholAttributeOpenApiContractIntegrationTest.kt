package app.integration.openapi

import com.fasterxml.jackson.databind.JsonNode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("admin_integration")
@DisplayName("[integration] 위스키 속성 OpenAPI 계약")
class AlcoholAttributeOpenApiContractIntegrationTest : OpenApiSpecTestSupport() {

	@Test
	@DisplayName("위스키 속성 9개 엔드포인트가 알코올 태그와 bearer 인증으로 문서화된다")
	fun attributeEndpointsDocumentPathsTagAndAuthentication() {
		val operations =
			operationsOf(fetchSpec()).filter { it.path.startsWith("/v1/alcohols/attributes") }

		assertThat(operations.map { it.endpoint() }).containsExactlyInAnyOrder(
			"GET /v1/alcohols/attributes",
			"GET /v1/alcohols/attributes/summary",
			"POST /v1/alcohols/attributes",
			"PUT /v1/alcohols/attributes/{id}",
			"DELETE /v1/alcohols/attributes/{id}",
			"POST /v1/alcohols/attributes/batch/delete",
			"GET /v1/alcohols/attributes/keys",
			"GET /v1/alcohols/attributes/keywords",
			"GET /v1/alcohols/attributes/enums"
		)
		operations.forEach { operation ->
			assertThat(operation.tag()).isEqualTo("알코올")
			assertThat(operation.summary()).isNotBlank()
			assertThat(operation.security().any { item -> item.has("bearerAuth") }).isTrue()
			assertThat(operation.hasEmptyDataSchema()).isFalse()
		}
	}

	@Test
	@DisplayName("묶음·추가·라벨 응답과 쓰기 요청 본문이 구체 스키마를 가리킨다")
	fun attributeSchemasReferenceConcreteTypes() {
		val spec = fetchSpec()
		val operations = operationsOf(spec).associateBy { it.endpoint() }

		assertThat(itemsRef(operations.getValue("GET /v1/alcohols/attributes/summary")))
			.endsWith("/AdminAlcoholAttributeSummaryItem")
		assertThat(dataRef(operations.getValue("POST /v1/alcohols/attributes")))
			.endsWith("/AdminAlcoholAttributeCreateResponse")
		assertThat(dataRef(operations.getValue("GET /v1/alcohols/attributes/enums")))
			.endsWith("/AdminAlcoholAttributeEnumsResponse")
		assertThat(requestRef(operations.getValue("POST /v1/alcohols/attributes")))
			.endsWith("/AdminAlcoholAttributeCreateRequest")
		assertThat(requestRef(operations.getValue("PUT /v1/alcohols/attributes/{id}")))
			.endsWith("/AdminAlcoholAttributeUpdateRequest")
		assertThat(requestRef(operations.getValue("POST /v1/alcohols/attributes/batch/delete")))
			.endsWith("/AdminAlcoholAttributeBatchDeleteRequest")

		val summary = spec.at("/components/schemas/AdminAlcoholAttributeSummaryItem")
		assertThat(propertyNamesOf(summary)).contains("attributeIds", "registeredCount", "alcoholIds")

		val alcoholIds = queryParameter(spec, operations.getValue("GET /v1/alcohols/attributes"), "alcoholIds")
		val schema = referencedSchema(spec, alcoholIds.path("schema"))
		assertThat(schema.path("type").asText()).isEqualTo("array")
	}

	private fun dataRef(operation: SpecOperation): String = operation.successSchema().at("/properties/data/\$ref").asText()

	private fun itemsRef(operation: SpecOperation): String = operation.successSchema().at("/properties/data/items/\$ref").asText()

	private fun requestRef(operation: SpecOperation): String = operation.definition.at("/requestBody/content/application~1json/schema/\$ref").asText()

	private fun queryParameter(
		spec: JsonNode,
		operation: SpecOperation,
		name: String
	): JsonNode {
		val parameters = buildList {
			addAll(operation.definition.path("parameters"))
			addAll(spec.at("/paths").path(operation.path).path("parameters"))
		}.map { node ->
			val ref = node.path("\$ref").asText()
			if (ref.isBlank()) node else spec.at(ref.removePrefix("#"))
		}
		return parameters.first { it.path("name").asText() == name }
	}
}
