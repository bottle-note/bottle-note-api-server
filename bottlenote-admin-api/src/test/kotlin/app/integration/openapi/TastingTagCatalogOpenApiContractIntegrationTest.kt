package app.integration.openapi

import com.fasterxml.jackson.databind.JsonNode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("admin_integration")
@DisplayName("[integration] Admin 테이스팅 태그 경량 조회 OpenAPI 계약")
class TastingTagCatalogOpenApiContractIntegrationTest : OpenApiSpecTestSupport() {
	@Test
	@DisplayName("경량 조회 세 경로는 아이콘 없는 목록과 아이콘 응답으로 문서화된다")
	fun catalogEndpointsDocumentLightweightResponses() {
		val spec = fetchSpec()
		val all = operation(spec, "GET /v1/tasting/tags/all")
		val ungrouped = operation(spec, "GET /v1/tasting/tags/ungrouped")
		val icon = operation(spec, "GET /v1/tasting/tags/{id}/icon")

		val allItem = referencedSchema(spec, all.successSchema().at("/properties/data/items"))
		val ungroupedItem = referencedSchema(spec, ungrouped.successSchema().at("/properties/data/items"))
		assertThat(propertyNamesOf(allItem))
			.containsExactly("id", "korName", "engName", "parentId", "alcoholCount", "groups")
		assertThat(propertyNamesOf(ungroupedItem))
			.containsExactly("id", "korName", "engName", "parentId", "alcoholCount", "groups")
		assertThat(propertyNamesOf(allItem)).doesNotContain("icon")

		val group = referencedSchema(spec, allItem.at("/properties/groups/items"))
		assertThat(propertyNamesOf(group)).containsExactly("groupCode", "groupName", "role")
		val role = referencedSchema(spec, group.at("/properties/role"))
		assertThat(propertyNamesOf(role)).containsExactly("code", "korName", "engName", "description")

		val iconData = referencedSchema(spec, icon.successSchema().at("/properties/data"))
		assertThat(propertyNamesOf(iconData)).containsExactly("icon")
		assertThat(childNamesOf(icon.definition.path("responses"))).contains("200", "404")
		assertThat(ungrouped.definition.at("/parameters").map { it.path("name").asText() }).contains("keyword")
	}

	@Test
	@DisplayName("경량 조회는 bearer 인증을 요구하고 기존 목록 스키마는 icon 필드를 유지한다")
	fun catalogEndpointsRequireBearerAndLegacyListKeepsIconField() {
		val spec = fetchSpec()
		listOf(
			"GET /v1/tasting/tags/all",
			"GET /v1/tasting/tags/ungrouped",
			"GET /v1/tasting/tags/{id}/icon"
		).forEach { endpoint ->
			val operation = operation(spec, endpoint)
			assertThat(operation.security().any { item -> item.has("bearerAuth") }).isTrue()
			assertThat(childNamesOf(operation.definition.path("responses"))).contains("200", "401", "403")
		}

		val legacy = operation(spec, "GET /v1/tasting-tags")
		val legacyItem = referencedSchema(spec, legacy.successSchema().at("/properties/data/items"))
		assertThat(propertyNamesOf(legacyItem)).contains("icon")
	}

	private fun operation(
		spec: JsonNode,
		endpoint: String
	): SpecOperation = operationsOf(spec).first { it.endpoint() == endpoint }

	private fun referencedSchema(
		spec: JsonNode,
		schema: JsonNode
	): JsonNode {
		val ref = schema.path("\$ref").asText()
		return if (ref.isBlank()) schema else spec.at(ref.removePrefix("#"))
	}
}
