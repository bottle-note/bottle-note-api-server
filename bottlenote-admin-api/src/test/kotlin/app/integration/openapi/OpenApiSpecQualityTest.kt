package app.integration.openapi

import com.fasterxml.jackson.databind.JsonNode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.util.regex.Pattern

/** 스펙 문서의 품질을 전수 검사한다. 예외를 두지 않으므로 모든 엔드포인트가 검사 대상이다. */
@Tag("admin_integration")
@DisplayName("[integration] Admin OpenAPI 스펙 품질")
class OpenApiSpecQualityTest : OpenApiSpecTestSupport() {

	@Test
	@DisplayName("엔드포인트의 태그는 클래스 이름이 아닌 도메인 이름이다")
	fun tagsUseDomainNames() {
		assertNoOperation(
			"태그가 컨트롤러 클래스명으로 남아 있습니다. 도메인 이름을 지정하세요"
		) { operation -> operation.tag().endsWith("-controller") }
	}

	@Test
	@DisplayName("모든 엔드포인트는 요약 문장을 갖는다")
	fun everyOperationHasSummary() {
		assertNoOperation("요약(summary)이 없습니다") { operation -> operation.summary().isBlank() }
	}

	@Test
	@DisplayName("요약은 메서드 이름을 그대로 옮긴 것이 아니다")
	fun summariesAreNotMethodNames() {
		assertNoOperation(
			"요약이 메서드 이름과 같습니다. 사람이 읽는 문장으로 쓰세요"
		) { operation -> operation.summary() == operation.operationId() }
	}

	@Test
	@DisplayName("응답 data는 실제 타입을 가리킨다")
	fun responseDataReferencesConcreteSchema() {
		assertNoOperation("응답 data가 빈 object입니다. 담기는 타입을 알려주세요") { operation -> operation.hasEmptyDataSchema() }
	}

	@Test
	@DisplayName("모든 파라미터는 schema, content 또는 참조를 갖는다")
	fun everyParameterHasSchemaSource() {
		val spec = fetchSpec()
		val pathViolations = spec.at("/paths").properties().flatMap { path ->
			parameterViolations("${path.key} [path]", path.value.path("parameters"))
		}
		val operationViolations = operationsOf(spec).flatMap { operation ->
			parameterViolations(operation.endpoint(), operation.definition.path("parameters"))
		}
		val violations = pathViolations + operationViolations

		assertThat(violations)
			.withFailMessage("파라미터에 schema, content 또는 \$ref가 없습니다:%n%s", joined(violations))
			.isEmpty()
	}

	@Test
	@DisplayName("components 하위 이름은 OpenAPI가 허용하는 식별자 형식이다")
	fun componentNamesUseAllowedIdentifiers() {
		val components = fetchSpec().at("/components")
		val violations =
			components.properties().flatMap { group ->
				childNamesOf(group.value)
					.filter { name -> !COMPONENT_NAME_PATTERN.matcher(name).matches() }
					.map { name -> "${group.key}.$name" }
			}

		assertThat(violations)
			.withFailMessage(
				"""
				components 하위 이름이 OpenAPI 규칙(%s)을 위반합니다. @Schema(name=...)이나 보안 스키마 이름에는 영문 식별자를 쓰고 한국어는 title이나 description으로 옮기세요. 위반하면 Scalar 같은 문서 도구가 스펙을 거부합니다:
				%s
				""".trimIndent(),
				COMPONENT_NAME_PATTERN.pattern(),
				joined(violations)
			)
			.isEmpty()
	}

	private fun parameterViolations(owner: String, parameters: JsonNode): List<String> = parameters
		.filter { parameter -> !hasParameterSchema(parameter) }
		.map { parameter -> "$owner - ${parameter.path("name").asText("<unnamed>")}" }

	private fun hasParameterSchema(parameter: JsonNode): Boolean = parameter.path("\$ref").asText("").isNotBlank() ||
		!parameter.path("schema").isEmpty ||
		!parameter.path("content").isEmpty

	/** 조건을 위반하는 엔드포인트가 하나도 없어야 한다. */
	private fun assertNoOperation(reason: String, violating: (SpecOperation) -> Boolean) {
		val violations = operationsOf(fetchSpec()).filter(violating).map { it.toString() }

		assertThat(violations).withFailMessage("%s:%n%s", reason, joined(violations)).isEmpty()
	}

	companion object {
		// OpenAPI가 components 하위 키에 허용하는 형식.
		private val COMPONENT_NAME_PATTERN: Pattern = Pattern.compile("^[a-zA-Z0-9._-]+$")
	}
}
