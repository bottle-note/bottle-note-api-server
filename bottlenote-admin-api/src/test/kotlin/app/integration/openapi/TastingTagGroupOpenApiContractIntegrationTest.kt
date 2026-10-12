package app.integration.openapi

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("admin_integration")
@DisplayName("[integration] 맛 그룹 OpenAPI 계약")
class TastingTagGroupOpenApiContractIntegrationTest : OpenApiSpecTestSupport() {

	@Test
	@DisplayName("맛 그룹 8개 엔드포인트가 테이스팅 태그 태그와 bearer 인증으로 문서화된다")
	fun groupEndpointsDocumentPathsTagAndAuthentication() {
		val operations = operationsOf(fetchSpec()).filter { it.path.startsWith("/v1/tasting/groups") }

		assertThat(operations.map { it.endpoint() }).containsExactlyInAnyOrder(
			"GET /v1/tasting/groups",
			"POST /v1/tasting/groups",
			"GET /v1/tasting/groups/{code}",
			"PUT /v1/tasting/groups/{code}",
			"DELETE /v1/tasting/groups/{code}",
			"POST /v1/tasting/groups/{code}/tags",
			"PUT /v1/tasting/groups/{code}/tags/{tagId}",
			"DELETE /v1/tasting/groups/{code}/tags/{tagId}"
		)
		operations.forEach { operation ->
			assertThat(operation.tag()).isEqualTo("테이스팅 태그")
			assertThat(operation.summary()).isNotBlank()
			assertThat(operation.security().any { item -> item.has("bearerAuth") }).isTrue()
			assertThat(operation.hasEmptyDataSchema()).isFalse()
		}
	}

	@Test
	@DisplayName("목록·상세·쓰기 응답과 요청 본문이 구체 스키마를 가리킨다")
	fun groupSchemasReferenceConcreteTypes() {
		val spec = fetchSpec()
		val operations = operationsOf(spec).associateBy { it.endpoint() }

		assertThat(dataRef(operations.getValue("GET /v1/tasting/groups")))
			.endsWith("/AdminTastingTagGroupListResponse")
		assertThat(dataRef(operations.getValue("GET /v1/tasting/groups/{code}")))
			.endsWith("/AdminTastingTagGroupItem")
		assertThat(dataRef(operations.getValue("POST /v1/tasting/groups")))
			.endsWith("/AdminResultResponse")
		assertThat(requestRef(operations.getValue("POST /v1/tasting/groups")))
			.endsWith("/AdminTastingTagGroupCreateRequest")
		assertThat(requestRef(operations.getValue("PUT /v1/tasting/groups/{code}")))
			.endsWith("/AdminTastingTagGroupUpdateRequest")
		assertThat(requestRef(operations.getValue("POST /v1/tasting/groups/{code}/tags")))
			.endsWith("/AdminTastingTagGroupTagsRequest")
		assertThat(requestRef(operations.getValue("PUT /v1/tasting/groups/{code}/tags/{tagId}")))
			.endsWith("/AdminTastingTagGroupTagRoleRequest")

		val tagItem = spec.at("/components/schemas/AdminTastingTagGroupTagItem")
		assertThat(propertyNamesOf(tagItem))
			.contains("tastingTagId", "korName", "role", "provider", "alcoholCount")
		assertThat(tagItem.at("/properties/role/\$ref").asText()).endsWith("/AdminCodeLabelItem")
		val label = spec.at("/components/schemas/AdminCodeLabelItem")
		assertThat(propertyNamesOf(label)).containsExactly("code", "korName", "engName", "description")
	}

	private fun dataRef(operation: SpecOperation): String = operation.successSchema().at("/properties/data/\$ref").asText()

	private fun requestRef(operation: SpecOperation): String = operation.definition.at("/requestBody/content/application~1json/schema/\$ref").asText()
}
