package app.bottlenote.alcohols.presentation.docs

import io.swagger.v3.oas.models.OpenAPI
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.stereotype.Component

@Component
class AlcoholAttributeSchemaCustomizer : OpenApiCustomizer {
	override fun customise(openApi: OpenAPI) {
		listOf("AdminAlcoholAttributeCreateRequest", "AdminAlcoholAttributeUpdateRequest", "AdminAlcoholAttributeItem").forEach { name ->
			val properties = openApi.components?.schemas?.get(name)?.properties
			properties?.get("groupId")?.types = setOf("integer", "null")
			properties?.get("allowedValues")?.types = setOf("array", "null")
			properties?.get("pattern")?.types = setOf("string", "null")
		}
	}
}
