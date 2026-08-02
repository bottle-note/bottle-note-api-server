package app.global.config

import app.bottlenote.global.exception.custom.code.ExceptionCode
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.responses.ApiResponse
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE

// 오류가 실리는 자리의 스키마를 한곳에 모은다.
// 성공 응답의 errors(빈 배열)와 실패 응답 본문이 같은 모양에서 갈라지므로 한곳에서 만들어야 서로 어긋나지 않는다.
internal object ErrorResponseSchema {

	private const val ITEM_NAME = "Error"
	private const val ENVELOPE_NAME = "ErrorResponse"

	private val ITEM_REF = Components.COMPONENTS_SCHEMAS_REF + ITEM_NAME
	private val ENVELOPE_REF = Components.COMPONENTS_SCHEMAS_REF + ENVELOPE_NAME

	// meta 자리의 예시. 값을 고정해 스펙을 만들 때마다 문서가 달라지지 않게 한다.
	val EXAMPLE_META: Map<String, Any> = linkedMapOf(
		"serverVersion" to "1.0.0",
		"serverPathVersion" to "v1",
		"serverEncoding" to "UTF-8",
		"serverResponseTime" to "2026-01-01T00:00:00"
	)

	// 스펙 전체에서 한 번만 정의하고 각 응답은 참조만 한다.
	fun registerInto(components: Components) {
		components.addSchemas(ITEM_NAME, errorItem())
		components.addSchemas(ENVELOPE_NAME, envelope())
	}

	// 실패 응답 하나. 본문 예시는 실제 예외 코드로 만들어 스펙과 실제 응답 모양이 어긋나지 않게 한다.
	fun response(sample: ExceptionCode, description: String): ApiResponse = ApiResponse()
		.description(description)
		.content(
			Content().addMediaType(
				APPLICATION_JSON_VALUE,
				MediaType()
					.schema(Schema<Any>().`$ref`(ENVELOPE_REF))
					.example(exampleBody(sample))
			)
		)

	private fun exampleBody(sample: ExceptionCode): Map<String, Any> = linkedMapOf(
		"success" to false,
		"code" to sample.httpStatus.value(),
		"data" to emptyList<Any>(),
		"errors" to listOf(exampleItem(sample)),
		"meta" to EXAMPLE_META
	)

	private fun exampleItem(sample: ExceptionCode): Map<String, Any> = linkedMapOf(
		"code" to (sample as Enum<*>).name,
		"status" to sample.httpStatus.name,
		"message" to sample.message
	)

	// 오류가 담기는 배열.
	fun errors(): Schema<*> = ArraySchema().items(Schema<Any>().`$ref`(ITEM_REF))

	// 성공 응답에 실리는 errors. 항상 비어 있으므로 담길 항목의 모양(items)은 적지 않는다.
	fun emptyErrors(): Schema<*> = ArraySchema().maxItems(0)

	private fun errorItem(): Schema<*> = ObjectSchema()
		.title("오류 항목")
		.addProperty(
			"code",
			StringSchema().description("오류 코드. 클라이언트가 화면 분기에 쓰는 값이다").example("ALCOHOL_ID_REQUIRED")
		)
		.addProperty("status", StringSchema().description("HTTP 상태 이름").example("BAD_REQUEST"))
		.addProperty("message", StringSchema().description("사람이 읽는 오류 설명").example("알코올 식별자는 필수입니다."))

	private fun envelope(): Schema<*> = ObjectSchema()
		.title("오류 응답")
		.addProperty("success", BooleanSchema().description("요청 처리 성공 여부").example(false))
		.addProperty("code", IntegerSchema().description("HTTP 상태 코드").example(400))
		.addProperty(
			"data",
			ArraySchema().items(ObjectSchema()).maxItems(0).description("오류 응답에서는 항상 빈 배열이다")
		)
		.addProperty("errors", errors().description("발생한 오류 목록"))
		.addProperty("meta", ObjectSchema().description("서버 버전, 응답 시각 등 부가 정보"))
}
