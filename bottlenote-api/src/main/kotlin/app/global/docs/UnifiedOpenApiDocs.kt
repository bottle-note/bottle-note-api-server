package app.global.docs

import app.global.admin.AdminApi
import app.global.admin.docs.AdminOpenApiDocs
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import org.springdoc.core.providers.SpringDocProviders
import org.springdoc.webmvc.api.OpenApiWebMvcResource
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Locale

// product 문서와 admin 문서를 합친 문서 하나를 만든다. docs 페이지는 이 문서만 연결한다.
// 기존 두 문서를 만드는 리소스의 결과(JSON)를 그대로 받아 합치므로 기존 문서의 생성 방식과 내용은 바뀌지 않는다.
@Component
class UnifiedOpenApiDocs(
	private val productOpenApiResource: OpenApiWebMvcResource,
	private val adminOpenApiDocs: AdminOpenApiDocs,
	private val springDocProviders: SpringDocProviders
) {
	private val merger = UnifiedOpenApiMerger()

	fun json(request: HttpServletRequest, locale: Locale): ByteArray = springDocProviders.jsonMapper().writeValueAsBytes(merge(request, PATH, locale).document)

	// springdoc의 YAML 출력과 같은 설정(USE_NATIVE_TYPE_ID 끔)으로 쓴다
	fun yaml(request: HttpServletRequest, locale: Locale): ByteArray {
		val mapper = springDocProviders.yamlMapper()
		(mapper.factory as YAMLFactory).configure(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID, false)
		return mapper.writeValueAsString(merge(request, YAML_PATH, locale).document).toByteArray(UTF_8)
	}

	fun merge(request: HttpServletRequest, docsPath: String, locale: Locale): UnifiedOpenApiMerger.Result {
		val mapper = springDocProviders.jsonMapper()
		// servers는 product 문서와 같은 방식(요청 URL에서 문서 경로를 뺀 값)으로 하나만 둔다
		val product = mapper.readTree(productOpenApiResource.openapiJson(request, docsPath, locale)) as ObjectNode
		val admin = mapper.readTree(adminOpenApiDocs.json(AdminDocsRequest(request, serverBaseUrl(request, docsPath)), locale)) as ObjectNode
		return merger.merge(product, admin, info())
	}

	// admin 문서 리소스는 요청 URL에서 admin 문서 경로를 빼서 servers를 정한다.
	// 같은 호스트로 admin 문서를 받을 때와 같은 URL을 보여 줘서, 리소스가 공유하는 servers 값이 admin 문서 요청과 달라지지 않게 한다.
	private class AdminDocsRequest(
		request: HttpServletRequest,
		private val serverBaseUrl: String
	) : HttpServletRequestWrapper(request) {
		override fun getRequestURL(): StringBuffer = StringBuffer(serverBaseUrl + AdminApi.DOCS_PATH)

		override fun getRequestURI(): String = AdminApi.DOCS_PATH
	}

	companion object {
		const val PATH = "/api/v1/openapi.json"
		const val YAML_PATH = "$PATH.yaml"

		private const val DESCRIPTION =
			"보틀노트 앱의 공개 API(/api/**)와 어드민 콘솔의 관리자 API(/admin/api/**)를 한 문서로 모았습니다.\n\n" +
				"모든 응답은 success, code, data, errors, meta를 갖는 공통 형식으로 감싸여 있고, 실제 결과값은 data 안에 담깁니다.\n\n" +
				"인증이 필요한 엔드포인트는 로그인 후 발급받은 액세스 토큰을 Authorization 헤더에 \"Bearer {토큰}\" 형태로 담아 호출합니다. " +
				"관리자 API는 관리자 로그인으로 받은 토큰을 씁니다.\n\n" +
				"두 API에서 같은 이름의 태그, 스키마, operationId가 다른 뜻으로 쓰이면 관리자 API 쪽 이름 앞에 Admin을 붙였습니다."

		private fun serverBaseUrl(request: HttpServletRequest, docsPath: String): String {
			val url = request.requestURL.toString()
			return if (url.endsWith(docsPath)) url.substring(0, url.length - docsPath.length) else url
		}

		private fun info(): ObjectNode = JsonNodeFactory.instance
			.objectNode()
			.put("title", "보틀노트 API")
			.put("description", DESCRIPTION)
			.put("version", "v1")
	}
}
