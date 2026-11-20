package app.global.docs

import io.swagger.v3.oas.annotations.Hidden
import jakarta.servlet.http.HttpServletRequest
import org.springdoc.core.utils.Constants.APPLICATION_OPENAPI_YAML
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.util.Locale

// product와 admin을 합친 OpenAPI 문서. 경로와 인증 정책은 product 문서와 같다(JSON은 공개, YAML은 기본 정책).
// 문서 자신은 product·admin 문서에 실리지 않게 숨긴다.
@Hidden
@RestController
class UnifiedOpenApiDocsController(
	private val unifiedOpenApiDocs: UnifiedOpenApiDocs
) {
	@GetMapping(UnifiedOpenApiDocs.PATH, produces = [MediaType.APPLICATION_JSON_VALUE])
	fun getUnifiedOpenApiJson(request: HttpServletRequest, locale: Locale): ByteArray = unifiedOpenApiDocs.json(request, locale)

	@GetMapping(UnifiedOpenApiDocs.YAML_PATH, produces = [APPLICATION_OPENAPI_YAML])
	fun getUnifiedOpenApiYaml(request: HttpServletRequest, locale: Locale): ByteArray = unifiedOpenApiDocs.yaml(request, locale)
}
