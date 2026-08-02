package app.global.config

import app.bottlenote.global.data.response.GlobalResponse
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.core.ResolvableType
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod

// 성공 응답을 GlobalResponse 공통 형식으로 감싼다.
// 각 엔드포인트는 data에 담길 타입만 표준 @ApiResponse로 알려주고, 이 클래스가 그 타입을 data 자리로 옮긴 뒤 공통 필드를 덧붙인다.
// ResponseEntity의 본문 타입을 구체적으로 선언한 엔드포인트(공통 형식을 쓰지 않는 DTO 그대로 반환)는 감싸기를 건너뛴다.
@Component
class GlobalResponseSchemaCustomizer : OperationCustomizer {

	override fun customize(operation: Operation, handlerMethod: HandlerMethod): Operation {
		val responses = operation.responses ?: return operation
		if (returnsBareDto(handlerMethod)) {
			return operation
		}
		responses.forEach { (statusCode, response) -> wrapIfSuccess(statusCode, response) }
		return operation
	}

	private fun returnsBareDto(handlerMethod: HandlerMethod): Boolean {
		val returnType = ResolvableType.forMethodReturnType(handlerMethod.method)
		if (!ResponseEntity::class.java.isAssignableFrom(returnType.toClass())) {
			return false
		}
		val bodyType = returnType.getGeneric(0).resolve()
		return bodyType != null &&
			bodyType != Any::class.java &&
			!GlobalResponse::class.java.isAssignableFrom(bodyType)
	}

	private fun wrapIfSuccess(statusCode: String, response: ApiResponse) {
		if (!statusCode.startsWith(SUCCESS_STATUS_PREFIX)) {
			return
		}
		val declared = declaredSchema(response)
		if (declared != null && alreadyWrapped(declared)) {
			return
		}
		val envelope = commonEnvelope(declared ?: ObjectSchema())
		response.content = Content().addMediaType(APPLICATION_JSON_VALUE, MediaType().schema(envelope))
	}

	// 엔드포인트가 알려준 data 타입.
	private fun declaredSchema(response: ApiResponse): Schema<*>? {
		val content = response.content ?: return null
		return content.values.map { it.schema }.firstOrNull { isMeaningful(it) }
	}

	// 타입 정보가 없는 빈 object와 공통 형식 자체는 힌트로 취급하지 않는다.
	private fun isMeaningful(schema: Schema<*>?): Boolean = schema != null && !isEnvelopeItself(schema) && !isEmptyObject(schema)

	private fun isEmptyObject(schema: Schema<*>): Boolean = OBJECT_TYPE == schema.type && schema.`get$ref`() == null && schema.properties.isNullOrEmpty()

	private fun isEnvelopeItself(schema: Schema<*>): Boolean = ENVELOPE_SCHEMA_REF == schema.`get$ref`()

	// 이미 공통 형식으로 적혀 있는지. isEnvelopeItself와 달리 참조가 아닌 펼쳐진 스키마를 본다.
	private fun alreadyWrapped(schema: Schema<*>): Boolean = schema.properties?.containsKey(DATA) == true

	private fun commonEnvelope(data: Schema<*>): Schema<*> = ObjectSchema()
		.addProperty(SUCCESS, BooleanSchema().description("요청 처리 성공 여부").example(true))
		.addProperty(CODE, IntegerSchema().description("HTTP 상태 코드").example(HTTP_OK))
		.addProperty(DATA, data.description("요청 결과 값"))
		.addProperty(ERRORS, ErrorResponseSchema.emptyErrors().description("오류 목록. 성공 시 비어 있다"))
		.addProperty(
			META,
			ObjectSchema().description("서버 버전, 응답 시각, 페이징 정보 등 부가 정보").example(ErrorResponseSchema.EXAMPLE_META)
		)

	companion object {
		private const val SUCCESS_STATUS_PREFIX = "2"
		private const val OBJECT_TYPE = "object"

		// 공통 형식의 code에 담기는 값. 성공 응답은 항상 200이다.
		private const val HTTP_OK = 200

		private const val SUCCESS = "success"
		private const val CODE = "code"
		private const val DATA = "data"
		private const val ERRORS = "errors"
		private const val META = "meta"

		// ResponseEntity<GlobalResponse>는 추론되는 스키마가 공통 형식 그 자체이므로 data에 담길 타입이 아니다.
		// 그대로 두면 공통 형식 안에 공통 형식이 중첩된다.
		private val ENVELOPE_SCHEMA_REF = Components.COMPONENTS_SCHEMAS_REF + GlobalResponse::class.java.simpleName
	}
}
