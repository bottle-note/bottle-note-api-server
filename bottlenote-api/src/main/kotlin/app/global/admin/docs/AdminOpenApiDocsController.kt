package app.global.admin.docs

import app.global.admin.AdminApi
import io.swagger.v3.oas.annotations.Hidden
import jakarta.servlet.http.HttpServletRequest
import org.springdoc.core.utils.Constants.APPLICATION_OPENAPI_YAML
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.util.Locale

// admin 앱에서 springdoc 기본 리소스가 제공하던 두 경로(JSON, YAML)를 같은 URL로 제공한다.
// 인증 정책도 admin 앱과 같다. JSON만 공개 경로로 등록되어 있고 YAML은 기본 정책(인증 필수)을 따른다.
@Hidden
@RestController
class AdminOpenApiDocsController(
	private val adminOpenApiDocs: AdminOpenApiDocs
) {
	@GetMapping(AdminApi.DOCS_PATH, produces = [MediaType.APPLICATION_JSON_VALUE])
	fun getAdminOpenApiJson(request: HttpServletRequest, locale: Locale): ByteArray = adminOpenApiDocs.json(request, locale)

	@GetMapping("${AdminApi.DOCS_PATH}.yaml", produces = [APPLICATION_OPENAPI_YAML])
	fun getAdminOpenApiYaml(request: HttpServletRequest, locale: Locale): ByteArray = adminOpenApiDocs.yaml(request, locale)
}
