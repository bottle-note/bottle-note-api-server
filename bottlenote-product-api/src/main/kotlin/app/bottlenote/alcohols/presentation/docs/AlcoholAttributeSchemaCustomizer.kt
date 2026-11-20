package app.bottlenote.alcohols.presentation.docs

import io.swagger.v3.oas.models.OpenAPI
import org.springdoc.core.customizers.OpenApiCustomizer

// product 문서에 섞이지 않도록 bean으로 등록하지 않는다. AdminOpenApiConfig가 admin 문서에만 연결한다.
class AlcoholAttributeSchemaCustomizer : OpenApiCustomizer {
	override fun customise(openApi: OpenAPI) {
		listOf("AdminAlcoholAttributeCreateRequest", "AdminAlcoholAttributeUpdateRequest", "AdminAlcoholAttributeItem").forEach { name ->
			val properties = openApi.components?.schemas?.get(name)?.properties
			properties?.get("groupId")?.types = setOf("integer", "null")
			properties?.get("allowedValues")?.types = setOf("array", "null")
			properties?.get("pattern")?.types = setOf("string", "null")
			properties?.get("allowMultipleValues")?.types = setOf("boolean")
		}
	}
}
