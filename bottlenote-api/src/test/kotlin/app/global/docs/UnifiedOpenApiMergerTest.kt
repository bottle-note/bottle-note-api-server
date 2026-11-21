package app.global.docs

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("unit")
@DisplayName("[unit] product·admin OpenAPI 문서 합치기")
class UnifiedOpenApiMergerTest {
	private val mapper = ObjectMapper()
	private val merger = UnifiedOpenApiMerger()
	private val info = mapper.createObjectNode().put("title", "unified")

	@Test
	@DisplayName("admin 경로를 합칠 때 /admin/api를 붙이고 product 경로와 servers는 그대로 둔다")
	fun prefixesAdminPathsAndKeepsProductPaths() {
		val result = merger.merge(product(), admin(), info).document

		assertThat(result.path("paths").fieldNames().asSequence().toList())
			.containsExactly("/api/v1/items", "/admin/api/v1/items", "/admin/api/v2/specs")
		assertThat(result.path("servers")).hasSize(1)
		assertThat(result.at("/servers/0/url").asText()).isEqualTo("http://localhost")
		assertThat(result.path("info")).isEqualTo(info)
	}

	@Test
	@DisplayName("이름과 내용이 같은 component를 받을 때 하나로 합친다")
	fun mergesIdenticalComponents() {
		val result = merger.merge(product(), admin(), info)

		assertThat(result.report.mergedComponents).contains("schemas/Error", "securitySchemes/bearerAuth")
		assertThat(result.document.at("/components/schemas/Error")).isEqualTo(product().at("/components/schemas/Error"))
		assertThat(result.document.at("/components/schemas/AdminError").isMissingNode).isTrue()
	}

	@Test
	@DisplayName("이름이 같고 내용이 다른 component를 받을 때 admin 쪽 이름을 바꾸고 admin의 참조만 함께 고친다")
	fun renamesDifferentAdminComponentAndRewritesRefs() {
		val result = merger.merge(product(), admin(), info)
		val document = result.document

		assertThat(result.report.renamedComponents).containsEntry("schemas/Item", "AdminItem")
		assertThat(document.at("/components/schemas/Item")).isEqualTo(product().at("/components/schemas/Item"))
		assertThat(document.at("/components/schemas/AdminItem")).isEqualTo(admin().at("/components/schemas/Item"))
		assertThat(document.at("/paths/~1api~1v1~1items/get/responses/200/content/application~1json/schema/\$ref").asText())
			.isEqualTo("#/components/schemas/Item")
		assertThat(document.at("/paths/~1admin~1api~1v1~1items/get/responses/200/content/application~1json/schema/\$ref").asText())
			.isEqualTo("#/components/schemas/AdminItem")
	}

	@Test
	@DisplayName("내용이 같은 component가 이름이 바뀐 component를 참조할 때 그 component도 admin 쪽 이름을 바꾼다")
	fun renamesComponentsThatReferenceRenamedOnes() {
		val result = merger.merge(product(), admin(), info)
		val document = result.document

		// Page는 두 문서에서 글자가 같지만 서로 다른 Item을 가리킨다
		assertThat(result.report.renamedComponents).containsEntry("schemas/Page", "AdminPage")
		assertThat(document.at("/components/schemas/Page/properties/items/\$ref").asText()).isEqualTo("#/components/schemas/Item")
		assertThat(document.at("/components/schemas/AdminPage/properties/items/\$ref").asText())
			.isEqualTo("#/components/schemas/AdminItem")
		assertThat(document.at("/paths/~1admin~1api~1v2~1specs/get/responses/200/content/application~1json/schema/\$ref").asText())
			.isEqualTo("#/components/schemas/AdminPage")
	}

	@Test
	@DisplayName("바꿀 이름이 이미 쓰이고 있을 때 숫자를 붙여 겹치지 않게 한다")
	fun appendsSuffixWhenRenamedComponentIsTaken() {
		val product = product()
		(product.at("/components/schemas") as ObjectNode).set<ObjectNode>("AdminItem", mapper.createObjectNode().put("type", "string"))

		val result = merger.merge(product, admin(), info)

		assertThat(result.report.renamedComponents).containsEntry("schemas/Item", "AdminItem_2")
		assertThat(result.document.at("/components/schemas/AdminItem/type").asText()).isEqualTo("string")
	}

	@Test
	@DisplayName("securitySchemes 이름이 같고 내용이 다를 때 admin 쪽 이름을 바꾸고 security requirement의 키도 고친다")
	fun renamesSecuritySchemeAndRequirementKeys() {
		val admin = admin()
		(admin.at("/components/securitySchemes/bearerAuth") as ObjectNode).put("description", "관리자 토큰")

		val result = merger.merge(product(), admin, info)
		val document = result.document

		assertThat(result.report.renamedComponents).containsEntry("securitySchemes/bearerAuth", "AdminbearerAuth")
		assertThat(document.at("/paths/~1admin~1api~1v1~1items/get/security/0").fieldNames().asSequence().toList())
			.containsExactly("AdminbearerAuth")
		assertThat(document.at("/paths/~1api~1v1~1items/get/security/0").fieldNames().asSequence().toList())
			.containsExactly("bearerAuth")
	}

	@Test
	@DisplayName("이름이 같은 태그를 받을 때 내용이 같으면 합치고 다르면 admin 쪽 이름을 바꿔 operation의 태그도 고친다")
	fun mergesOrRenamesTags() {
		val result = merger.merge(product(), admin(), info)
		val document = result.document

		assertThat(result.report.mergedTags).containsExactly("공통")
		assertThat(result.report.renamedTags).containsEntry("인증", "Admin 인증")
		assertThat(document.path("tags").map { it.path("name").asText() }).containsExactly("인증", "공통", "Admin 인증", "알코올")
		assertThat(document.at("/paths/~1admin~1api~1v1~1items/get/tags/0").asText()).isEqualTo("Admin 인증")
		assertThat(document.at("/paths/~1api~1v1~1items/get/tags/0").asText()).isEqualTo("인증")
	}

	@Test
	@DisplayName("operationId가 겹칠 때 admin 쪽 operationId 앞에 admin을 붙여 유일하게 만든다")
	fun renamesCollidingOperationIds() {
		val result = merger.merge(product(), admin(), info)
		val document = result.document

		assertThat(result.report.renamedOperationIds).containsExactly(entry("getItems", "adminGetItems"))
		assertThat(document.at("/paths/~1api~1v1~1items/get/operationId").asText()).isEqualTo("getItems")
		assertThat(document.at("/paths/~1admin~1api~1v1~1items/get/operationId").asText()).isEqualTo("adminGetItems")
		assertThat(document.at("/paths/~1admin~1api~1v2~1specs/get/operationId").asText()).isEqualTo("getSpecs")
	}

	@Test
	@DisplayName("합칠 때 입력으로 받은 두 문서는 바꾸지 않는다")
	fun doesNotMutateInputs() {
		val product = product()
		val admin = admin()

		merger.merge(product, admin, info)

		assertThat(product).isEqualTo(product())
		assertThat(admin).isEqualTo(admin())
	}

	@Test
	@DisplayName("OpenAPI 버전이 다를 때 합치지 않고 실패한다")
	fun rejectsDifferentOpenApiVersions() {
		val admin = admin().put("openapi", "3.0.1")

		assertThatThrownBy { merger.merge(product(), admin, info) }.isInstanceOf(IllegalArgumentException::class.java)
	}

	@Test
	@DisplayName("다루지 않는 최상위 항목이 두 문서에서 다를 때 덮어쓰지 않고 실패한다")
	fun rejectsConflictingTopLevelFields() {
		val product = product().put("jsonSchemaDialect", "a")
		val admin = admin().put("jsonSchemaDialect", "b")

		assertThatThrownBy { merger.merge(product, admin, info) }.isInstanceOf(IllegalStateException::class.java)
	}

	private fun product(): ObjectNode = mapper.readTree(PRODUCT) as ObjectNode

	private fun admin(): ObjectNode = mapper.readTree(ADMIN) as ObjectNode

	companion object {
		private val PRODUCT = """
		{
		  "openapi": "3.1.0",
		  "info": {"title": "product"},
		  "servers": [{"url": "http://localhost", "description": "Generated server url"}],
		  "tags": [{"name": "인증", "description": "소셜 로그인"}, {"name": "공통", "description": "공통 기능"}],
		  "paths": {
		    "/api/v1/items": {
		      "get": {
		        "tags": ["인증"],
		        "operationId": "getItems",
		        "security": [{"bearerAuth": []}],
		        "responses": {"200": {"content": {"application/json": {"schema": {"${'$'}ref": "#/components/schemas/Item"}}}}}
		      }
		    }
		  },
		  "components": {
		    "schemas": {
		      "Item": {"type": "object", "properties": {"userId": {"type": "integer"}}},
		      "Page": {"type": "object", "properties": {"items": {"${'$'}ref": "#/components/schemas/Item"}}},
		      "Error": {"type": "object", "properties": {"message": {"type": "string"}}}
		    },
		    "securitySchemes": {"bearerAuth": {"type": "http", "scheme": "bearer"}}
		  }
		}
		"""

		private val ADMIN = """
		{
		  "openapi": "3.1.0",
		  "info": {"title": "admin"},
		  "servers": [{"url": "http://localhost/admin/api", "description": "Generated server url"}],
		  "tags": [{"name": "인증", "description": "관리자 로그인"}, {"name": "공통", "description": "공통 기능"}, {"name": "알코올", "description": "위스키 관리"}],
		  "paths": {
		    "/v1/items": {
		      "get": {
		        "tags": ["인증"],
		        "operationId": "getItems",
		        "security": [{"bearerAuth": []}],
		        "responses": {"200": {"content": {"application/json": {"schema": {"${'$'}ref": "#/components/schemas/Item"}}}}}
		      }
		    },
		    "/v2/specs": {
		      "get": {
		        "tags": ["알코올"],
		        "operationId": "getSpecs",
		        "responses": {"200": {"content": {"application/json": {"schema": {"${'$'}ref": "#/components/schemas/Page"}}}}}
		      }
		    }
		  },
		  "components": {
		    "schemas": {
		      "Item": {"type": "object", "properties": {"alcoholId": {"type": "integer"}}},
		      "Page": {"type": "object", "properties": {"items": {"${'$'}ref": "#/components/schemas/Item"}}},
		      "Error": {"type": "object", "properties": {"message": {"type": "string"}}}
		    },
		    "securitySchemes": {"bearerAuth": {"type": "http", "scheme": "bearer"}}
		  }
		}
		"""
	}
}
