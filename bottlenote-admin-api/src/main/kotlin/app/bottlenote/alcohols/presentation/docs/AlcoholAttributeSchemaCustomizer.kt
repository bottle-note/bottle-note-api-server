package app.bottlenote.alcohols.presentation.docs

import io.swagger.v3.oas.models.OpenAPI
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.stereotype.Component

@Component
class AlcoholAttributeSchemaCustomizer : OpenApiCustomizer {
	override fun customise(openApi: OpenAPI) {
		listOf("AdminAlcoholAttributeCreateRequest", "AdminAlcoholAttributeUpdateRequest", "AdminAlcoholAttributeItem").forEach { name ->
			openApi.components?.schemas?.get(name)?.properties?.get("groupId")?.types = setOf("integer", "null")
		}
	}
}
