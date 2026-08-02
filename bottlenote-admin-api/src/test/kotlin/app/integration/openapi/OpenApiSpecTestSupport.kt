package app.integration.openapi

import app.IntegrationTestSupport
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.MissingNode
import org.springframework.beans.factory.annotation.Value
import java.nio.charset.StandardCharsets.UTF_8

/** 생성된 Admin OpenAPI 스펙을 읽어 검사하는 테스트들의 공통 기반. */
abstract class OpenApiSpecTestSupport : IntegrationTestSupport() {

	// 스펙 경로의 SSOT는 springdoc.api-docs.path 프로퍼티다
	@Value("\${springdoc.api-docs.path}")
	protected lateinit var specPath: String

	protected fun fetchSpec(): JsonNode {
		val result = mockMvcTester.get().uri(specPath).exchange()
		return mapper.readTree(result.response.getContentAsString(UTF_8))
	}

	/** 스펙의 모든 엔드포인트를 평탄하게 펼친다. */
	protected fun operationsOf(spec: JsonNode): List<SpecOperation> = spec.at("/paths").properties().flatMap { path ->
		path.value.properties().map { method -> SpecOperation(path.key, method.key, method.value) }
	}

	/** 위반 목록을 실패 메시지에 담을 형태로 잇는다. */
	protected fun joined(violations: List<String>): String = violations.joinToString(System.lineSeparator())

	/** 노드의 직접 자식 이름. */
	protected fun childNamesOf(node: JsonNode): List<String> = node.properties().map { it.key }

	/** 스키마 노드가 선언한 property 이름. */
	protected fun propertyNamesOf(node: JsonNode): List<String> = childNamesOf(node.path("properties"))

	/** 스펙에 실린 하나의 엔드포인트. */
	protected data class SpecOperation(val path: String, val method: String, val definition: JsonNode) {

		/** 200 응답 본문의 스키마. 공통 형식이면 그 형식, 아니면 DTO 참조. */
		fun successSchema(): JsonNode {
			val content = definition.at("/responses/200/content")
			if (content.isMissingNode || content.isEmpty) {
				return MissingNode.getInstance()
			}
			return content.properties().iterator().next().value.path("schema")
		}

		/** HTTP 메서드와 경로를 합친 식별자. */
		fun endpoint(): String = "${method.uppercase()} $path"

		/** 문서에 선언된 보안 요구사항. 선언이 없으면 missing 노드. */
		fun security(): JsonNode = definition.path("security")

		fun tag(): String {
			val tags = definition.at("/tags")
			return if (tags.isArray && !tags.isEmpty) tags.get(0).asText() else ""
		}

		fun summary(): String = definition.path("summary").asText("")

		fun operationId(): String = definition.path("operationId").asText("")

		/** 공통 형식의 data 자리에 담긴 스키마. */
		private fun dataSchema(): JsonNode = successSchema().path("properties").path("data")

		fun hasEmptyDataSchema(): Boolean {
			val data = dataSchema()
			return OBJECT_TYPE == data.path("type").asText() && !data.has("\$ref") && !data.has("properties")
		}

		override fun toString(): String = "${method.uppercase()} $path (${tag()})"

		companion object {
			private const val OBJECT_TYPE = "object"
		}
	}
}
