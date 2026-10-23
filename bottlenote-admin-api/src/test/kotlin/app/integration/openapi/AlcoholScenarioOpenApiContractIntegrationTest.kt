package app.integration.openapi

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("admin_integration")
@DisplayName("시나리오 OpenAPI를 생성할 때 실제 경로와 타입 계약을 제공한다")
class AlcoholScenarioOpenApiContractIntegrationTest : OpenApiSpecTestSupport() {
	private val root = "/v1/attribute/groups/{groupId}/scenarios"

	@Test
	@DisplayName("관리 API를 문서화할 때 모든 CRUD·버전·게시·미리보기 경로에 Admin 인증을 요구한다")
	fun documentsEveryAdminEndpointWithConcreteResponsesAndSecurity() {
		val spec = fetchSpec()
		val endpoints = listOf(
			"GET $root", "POST $root", "GET $root/{scenarioId}", "PUT $root/{scenarioId}", "DELETE $root/{scenarioId}",
			"POST $root/{scenarioId}/versions", "GET $root/{scenarioId}/versions/{versionId}",
			"PUT $root/{scenarioId}/versions/{versionId}", "DELETE $root/{scenarioId}/versions/{versionId}",
			"POST $root/{scenarioId}/versions/{versionId}/publish", "POST $root/{scenarioId}/versions/{versionId}/preview"
		)
		for (endpoint in endpoints) {
			val op = operation(spec, endpoint)
			assertThat(op.security().any { it.has("bearerAuth") }).isTrue()
			assertThat(op.hasEmptyDataSchema()).isFalse()
			assertThat(op.summary()).isNotBlank()
		}
		assertThat(operationsOf(spec).none { it.path.contains("scenario-runs") }).isTrue()
		val request = referencedSchema(spec, operation(spec, "PUT $root/{scenarioId}/versions/{versionId}").definition.at("/requestBody/content/application~1json/schema"))
		assertThat(propertyNamesOf(request)).containsExactlyInAnyOrder("expectedLockVersion", "definition")
		val publish = referencedSchema(spec, operation(spec, "POST $root/{scenarioId}/versions/{versionId}/publish").definition.at("/requestBody/content/application~1json/schema"))
		assertThat(propertyNamesOf(publish)).containsExactlyInAnyOrder("expectedScenarioLockVersion", "expectedLockVersion")
	}

	@Test
	@DisplayName("정의 계약을 문서화할 때 선택지 ID와 JSON 타입·범위 비교 의미를 명시한다")
	fun documentsSchemaOneScalarTypesAndRangeSemantics() {
		val spec = fetchSpec()
		val schemas = spec.at("/components/schemas")
		val definition = schemas["ScenarioDefinitionRequest"]
		assertThat(definition["description"].asText()).contains("음수", "기존 ID", "배열 순서")
		assertThat(definition.at("/properties/questions/maxItems").asInt()).isEqualTo(100)
		val question = schemas["ScenarioQuestionItem"]
		assertThat(question.at("/properties/sourceKind/enum").map { it.asText() }).containsExactlyInAnyOrder("ATTRIBUTE", "CATEGORY", "TASTING_GROUP", "ABV")
		assertThat(question.at("/properties/inputKind/enum").map { it.asText() }).containsExactlyInAnyOrder("SINGLE_CHOICE", "NUMBER_RANGE")
		assertThat(schemas["ScenarioInputConfig"]["description"].asText()).contains("ANY", "min<=max", "step>0", "SINGLE_CHOICE", "NUMBER_RANGE")
		assertThat(schemas["ScenarioInputConfig"]["additionalProperties"].asBoolean()).isFalse()
		val types = schemas["ScenarioChoice"].at("/properties/value/oneOf").map { it["type"]?.asText() ?: it["types"]?.toString() }
		assertThat(types).contains("string", "number", "boolean")
		assertThat(schemas["ScenarioPredicate"]["description"].asText()).contains("choiceId", "하한(min)>=value", "상한(max)<=value", "ALWAYS", "ANY")
		assertThat(schemas["ScenarioPredicate"].at("/properties/operator/enum").map { it.asText() }).containsExactlyInAnyOrder("EQ", "GTE", "LTE", "ALWAYS")
	}

	@Test
	@DisplayName("미리보기를 문서화할 때 질문별 조건과 답변 폐기·과거 카드 집계 계약을 제공한다")
	fun documentsPreviewInvalidationAndPrefixBasedCounts() {
		val schemas = fetchSpec().at("/components/schemas")
		val request = schemas["AdminScenarioPreviewRequest"]
		assertThat(propertyNamesOf(request)).contains("answers", "changedQuestionId", "viewQuestionId", "expectedLockVersion", "page", "size")
		assertThat(request["description"].asText()).contains("직전", "폐기", "AND")
		assertThat(propertyNamesOf(schemas["ScenarioPreview"])).contains("path", "nextQuestionId", "completed", "answers", "discardedQuestionIds", "contributions", "candidates")
		assertThat(propertyNamesOf(schemas["ScenarioContribution"])).contains("questionId", "source", "attributeId", "operator", "value")
		assertThat(schemas["ScenarioAnswer"]["description"].asText()).contains("CHOICE", "NUMBER_RANGE", "ANY", "step")
	}
}
