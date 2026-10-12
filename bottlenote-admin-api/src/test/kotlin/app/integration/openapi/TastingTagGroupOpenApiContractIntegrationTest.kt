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

		assertThat(dataRef(operation(spec, "GET /v1/tasting/groups")))
			.endsWith("/AdminTastingTagGroupListResponse")
		assertThat(dataRef(operation(spec, "GET /v1/tasting/groups/{code}")))
			.endsWith("/AdminTastingTagGroupItem")
		assertThat(dataRef(operation(spec, "POST /v1/tasting/groups")))
			.endsWith("/AdminResultResponse")
		assertThat(requestRef(operation(spec, "POST /v1/tasting/groups")))
			.endsWith("/AdminTastingTagGroupCreateRequest")
		assertThat(requestRef(operation(spec, "PUT /v1/tasting/groups/{code}")))
			.endsWith("/AdminTastingTagGroupUpdateRequest")
		assertThat(requestRef(operation(spec, "POST /v1/tasting/groups/{code}/tags")))
			.endsWith("/AdminTastingTagGroupTagsRequest")
		assertThat(requestRef(operation(spec, "PUT /v1/tasting/groups/{code}/tags/{tagId}")))
			.endsWith("/AdminTastingTagGroupTagRoleRequest")

		val tagItem = referencedSchema(spec, spec.at("/components/schemas/AdminTastingTagGroupTagItem"))
		assertThat(propertyNamesOf(tagItem))
			.contains("tastingTagId", "korName", "role", "provider", "alcoholCount")
		val role = referencedSchema(spec, tagItem.at("/properties/role"))
		assertThat(propertyNamesOf(role)).containsExactly("code", "korName", "engName", "description")
	}

	@Test
	@DisplayName("없는 자원은 404, 중복과 태그 있는 삭제는 409로 문서화된다")
	fun groupEndpointsDocumentNotFoundAndConflict() {
		val spec = fetchSpec()

		assertThat(responseCodes(operation(spec, "GET /v1/tasting/groups/{code}"))).contains("404")
		assertThat(responseCodes(operation(spec, "POST /v1/tasting/groups"))).contains("409")
		assertThat(responseCodes(operation(spec, "PUT /v1/tasting/groups/{code}"))).contains("404", "409")
		assertThat(responseCodes(operation(spec, "DELETE /v1/tasting/groups/{code}"))).contains("404", "409")
		assertThat(responseCodes(operation(spec, "POST /v1/tasting/groups/{code}/tags"))).contains("404")
		assertThat(responseCodes(operation(spec, "PUT /v1/tasting/groups/{code}/tags/{tagId}"))).contains("404")
		assertThat(responseCodes(operation(spec, "DELETE /v1/tasting/groups/{code}/tags/{tagId}"))).contains("404")
	}

	private fun dataRef(operation: SpecOperation): String = operation.successSchema().at("/properties/data/\$ref").asText()

	private fun requestRef(operation: SpecOperation): String = operation.definition.at("/requestBody/content/application~1json/schema/\$ref").asText()

	private fun responseCodes(operation: SpecOperation): List<String> = childNamesOf(operation.definition.path("responses"))
}
