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
	@DisplayName("복합 검색은 조건 배열과 연산자 및 AND OR를 문서화한다")
	fun compoundSearchDocumentsConditions() {
		val spec = fetchSpec()
		val operation = operationsOf(spec).first { it.endpoint() == "POST /v1/alcohols/attributes/alcohols/search" }
		assertThat(requestRef(operation)).endsWith("/AdminAlcoholCompoundSearchRequest")
		val request = spec.at("/components/schemas/AdminAlcoholCompoundSearchRequest")
		assertThat(request.at("/properties/conditions/maxItems").asInt()).isEqualTo(20)
		assertThat(request.at("/properties/match/enum").map { it.asText() }).containsExactly("AND", "OR")
		val condition = spec.at("/components/schemas/AdminAlcoholAttributeCondition")
		assertThat(condition.path("required").map { it.asText() }).contains("key")
		assertThat(condition.at("/properties/operator/enum").map { it.asText() }).containsExactlyInAnyOrder("EQ", "NE", "GT", "GTE", "LT", "LTE", "CONTAINS", "EXISTS", "NOT_EXISTS")
	}

	@Test
	@DisplayName("위스키 속성 16개 엔드포인트가 알코올 태그와 bearer 인증으로 문서화된다")
	fun attributeEndpointsDocumentPathsTagAndAuthentication() {
		val operations =
			operationsOf(fetchSpec()).filter { it.path.startsWith("/v1/alcohols/attributes") }

		assertThat(operations.map { it.endpoint() }).containsExactlyInAnyOrder(
			"GET /v1/alcohols/attributes",
			"GET /v1/alcohols/attributes/summary",
			"GET /v1/alcohols/attributes/alcohols",
			"POST /v1/alcohols/attributes/alcohols/search",
			"POST /v1/alcohols/attributes",
			"PUT /v1/alcohols/attributes/{id}",
			"DELETE /v1/alcohols/attributes/{id}",
			"POST /v1/alcohols/attributes/batch/delete",
			"GET /v1/alcohols/attributes/keys",
			"GET /v1/alcohols/attributes/keywords",
			"GET /v1/alcohols/attributes/enums",
			"POST /v1/alcohols/attributes/batch",
			"POST /v1/alcohols/attributes/mappings/batch",
			"GET /v1/alcohols/attributes/mappings",
			"DELETE /v1/alcohols/attributes/mappings/{id}",
			"PUT /v1/alcohols/attributes/mappings/{id}"
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
			.endsWith("/AdminAlcoholAttributeItem")
		assertThat(dataRef(operations.getValue("GET /v1/alcohols/attributes/enums")))
			.endsWith("/AdminAlcoholAttributeEnumsResponse")
		assertThat(requestRef(operations.getValue("POST /v1/alcohols/attributes")))
			.endsWith("/AdminAlcoholAttributeCreateRequest")
		assertThat(requestRef(operations.getValue("PUT /v1/alcohols/attributes/{id}")))
			.endsWith("/AdminAlcoholAttributeUpdateRequest")
		assertThat(requestRef(operations.getValue("POST /v1/alcohols/attributes/batch/delete")))
			.endsWith("/AdminAlcoholAttributeBatchDeleteRequest")

		val summary = spec.at("/components/schemas/AdminAlcoholAttributeSummaryItem")
		assertThat(propertyNamesOf(summary)).contains("mappingIds", "registeredCount", "alcoholIds", "attribute")

		val item = spec.at("/components/schemas/AdminAlcoholAttributeItem")
		assertThat(propertyNamesOf(item)).contains("displayName", "unit", "createPrincipal", "lastModifyPrincipal")
		assertThat(propertyNamesOf(item)).doesNotContain("alcoholId", "basedAt", "operator", "value")
		assertThat(requestRef(operations.getValue("POST /v1/alcohols/attributes/batch"))).endsWith("/AdminAlcoholAttributeBulkRequest")
		assertThat(requestRef(operations.getValue("POST /v1/alcohols/attributes/mappings/batch"))).endsWith("/AdminAlcoholAttributeMappingBulkRequest")
		assertThat(dataRef(operations.getValue("POST /v1/alcohols/attributes/mappings/batch")))
			.endsWith("/AdminAlcoholAttributeMappingBulkResponse")
		val alcoholIds = queryParameter(spec, operations.getValue("GET /v1/alcohols/attributes"), "alcoholIds")
		val schema = referencedSchema(spec, alcoholIds.path("schema"))
		assertThat(schema.path("type").asText()).isEqualTo("array")
	}

	@Test
	@DisplayName("집계 API는 실제 사용하는 주류 ID만 검색 파라미터로 노출한다")
	fun summaryOnlyExposesAlcoholIds() {
		val operation = operationsOf(fetchSpec()).first { it.endpoint() == "GET /v1/alcohols/attributes/summary" }
		val names = operation.definition.path("parameters").map { it.path("name").asText() }
		assertThat(names).containsExactly("alcoholIds")
	}

	@Test
	@DisplayName("정의에는 값이 없고 연결 생성·수정·응답에는 값이 있다")
	fun mappingOwnsValueContract() {
		val spec = fetchSpec()
		for (name in listOf("AdminAlcoholAttributeCreateRequest", "AdminAlcoholAttributeUpdateRequest", "AdminAlcoholAttributeItem")) {
			assertThat(propertyNamesOf(spec.at("/components/schemas/$name"))).doesNotContain("value", "operator")
		}
		assertThat(propertyNamesOf(spec.at("/components/schemas/AdminAlcoholAttributeMappingItem"))).contains("value", "attributeId", "alcoholId")
		val update = spec.at("/components/schemas/AdminAlcoholAttributeMappingUpdateRequest")
		assertThat(propertyNamesOf(update)).containsExactly("value")
		assertThat(update.path("required").map { it.asText() }).contains("value")
		val operations = operationsOf(spec).associateBy { it.endpoint() }
		assertThat(requestRef(operations.getValue("PUT /v1/alcohols/attributes/mappings/{id}"))).endsWith("/AdminAlcoholAttributeMappingUpdateRequest")
		assertThat(dataRef(operations.getValue("PUT /v1/alcohols/attributes/mappings/{id}"))).endsWith("/AdminAlcoholAttributeMappingItem")
		val operator = queryParameter(spec, operations.getValue("GET /v1/alcohols/attributes/alcohols"), "operator")
		assertThat(operator).isNotNull()
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
