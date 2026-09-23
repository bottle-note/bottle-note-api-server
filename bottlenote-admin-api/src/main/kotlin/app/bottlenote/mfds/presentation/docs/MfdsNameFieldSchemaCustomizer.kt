package app.bottlenote.mfds.presentation.docs

import app.bottlenote.mfds.constant.MfdsNameFieldDescriptions
import io.swagger.v3.oas.models.OpenAPI
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.stereotype.Component

// 공유 DTO에는 문서 어노테이션이 없으므로 수입 신고 응답 스키마의 이름 필드 설명을 여기서 붙인다.
@Component
class MfdsNameFieldSchemaCustomizer : OpenApiCustomizer {

	override fun customise(openApi: OpenAPI) {
		val schemas = openApi.components?.schemas ?: return
		TARGET_SCHEMAS.mapNotNull { schemas[it] }.forEach { schema ->
			MfdsNameFieldDescriptions.BY_PROPERTY.forEach { (property, description) ->
				schema.properties?.get(property)?.description = description
			}
		}
	}

	private companion object {
		val TARGET_SCHEMAS = listOf("MfdsDeclarationListItem", "MfdsDeclarationDetailResponse")
	}
}
