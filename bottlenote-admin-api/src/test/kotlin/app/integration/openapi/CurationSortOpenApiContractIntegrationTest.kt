package app.integration.openapi

import com.fasterxml.jackson.databind.JsonNode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("admin_integration")
@DisplayName("[integration] Admin curation 선택 정렬 OpenAPI 계약")
class CurationSortOpenApiContractIntegrationTest : OpenApiSpecTestSupport() {

	@Test
	@DisplayName("목록과 피드는 선택 정렬 query enum과 기본값을 명시한다")
	fun listAndFeed_exposeSelectableSortParameterSchemas() {
		val spec = fetchSpec()
		listOf("GET /v2/curations", "GET /v2/curations/feed").forEach { endpoint ->
			val operation = operationsOf(spec).first { it.endpoint() == endpoint }
			assertSortParameter(operation.definition, "sortType", listOf("EXPOSURE_START_DATE", "DISPLAY_ORDER"), "EXPOSURE_START_DATE")
			assertSortParameter(operation.definition, "sortOrder", listOf("ASC", "DESC"), "DESC")
		}
	}

	private fun assertSortParameter(
		definition: JsonNode,
		name: String,
		expectedEnum: List<String>,
		expectedDefault: String
	) {
		val schema = definition.path("parameters")
			.first { it.path("name").asText() == name }
			.path("schema")

		assertThat(schema.path("type").asText()).isEqualTo("string")
		assertThat(schema.path("enum").map(JsonNode::asText)).containsExactlyInAnyOrderElementsOf(expectedEnum)
		assertThat(schema.path("default").asText()).isEqualTo(expectedDefault)
	}
}
