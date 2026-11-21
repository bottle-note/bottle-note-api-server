package app.global.admin.docs

import app.bottlenote.alcohols.presentation.docs.AlcoholAttributeSchemaCustomizer
import app.bottlenote.alcohols.presentation.docs.AlcoholScenarioSchemaCustomizer
import app.bottlenote.mfds.presentation.docs.AdminMfdsNameFieldSchemaCustomizer
import app.global.admin.AdminApi
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Paths
import jakarta.servlet.http.HttpServletRequest
import org.springdoc.core.customizers.OpenApiBuilderCustomizer
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springdoc.core.customizers.OperationCustomizer
import org.springdoc.core.customizers.ServerBaseUrlCustomizer
import org.springdoc.core.customizers.SpringDocCustomizers
import org.springdoc.core.filters.OpenApiMethodFilter
import org.springdoc.core.properties.SpringDocConfigProperties
import org.springdoc.core.providers.JavadocProvider
import org.springdoc.core.providers.SpringDocProviders
import org.springdoc.core.service.AbstractRequestService
import org.springdoc.core.service.GenericResponseService
import org.springdoc.core.service.OpenAPIService
import org.springdoc.core.service.OperationService
import org.springdoc.core.service.SecurityService
import org.springdoc.core.utils.PropertyResolverUtils
import org.springdoc.webmvc.api.OpenApiWebMvcResource
import org.springframework.beans.factory.ObjectFactory
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import java.util.Locale
import java.util.Optional

// admin 앱의 /admin/api/v1/openapi.admin.json 문서를 같은 내용으로 만든다.
// product 문서는 springdoc 기본 리소스가 그대로 맡고, admin 문서는 별도 리소스 인스턴스가 admin 컨트롤러와 admin customizer만으로 만든다.
// 리소스를 bean으로 등록하지 않는 이유: OpenApiWebMvcResource bean이 생기면 springdoc 기본 리소스가 물러나고(@ConditionalOnMissingBean),
// 상위 타입의 @GetMapping(springdoc.api-docs.path)이 product 문서 경로와 겹친다.
@Component
class AdminOpenApiDocs(
	private val applicationContext: ApplicationContext,
	requestBuilder: AbstractRequestService,
	responseBuilder: GenericResponseService,
	operationParser: OperationService,
	private val springDocConfigProperties: SpringDocConfigProperties,
	springDocProviders: SpringDocProviders,
	private val securityService: SecurityService,
	private val propertyResolverUtils: PropertyResolverUtils,
	private val openApiBuilderCustomizers: Optional<List<OpenApiBuilderCustomizer>>,
	private val serverBaseUrlCustomizers: Optional<List<ServerBaseUrlCustomizer>>,
	private val javadocProvider: Optional<JavadocProvider>,
	springDocCustomizers: SpringDocCustomizers
) {
	private val resource = OpenApiWebMvcResource(
		GROUP_NAME,
		ObjectFactory { newOpenApiService() },
		requestBuilder,
		responseBuilder,
		operationParser,
		springDocConfigProperties,
		springDocProviders,
		adminCustomizers(springDocCustomizers)
	)

	// servers는 요청 URL에서 admin 앱의 springdoc.api-docs.path를 뺀 값이므로 admin 앱과 같은 {origin}/admin/api가 된다.
	fun json(request: HttpServletRequest, locale: Locale): ByteArray = resource.openapiJson(request, ADMIN_APP_API_DOCS_PATH, locale)

	fun yaml(request: HttpServletRequest, locale: Locale): ByteArray = resource.openapiYaml(request, "$ADMIN_APP_API_DOCS_PATH.yaml", locale)

	// springdoc 기본 설정에서 OpenAPIService는 문서마다 새로 만든다(prototype). 같은 방식으로 admin 전용 OpenAPI를 기준으로 만든다.
	private fun newOpenApiService(): OpenAPIService {
		val service = OpenAPIService(
			Optional.of(AdminOpenApiConfig.adminOpenApi()),
			securityService,
			springDocConfigProperties,
			propertyResolverUtils,
			openApiBuilderCustomizers,
			serverBaseUrlCustomizers,
			javadocProvider
		)
		service.setApplicationContext(applicationContext)
		return service
	}

	// admin 앱의 문서에는 admin customizer bean과 springdoc이 기본으로 등록하는 customizer(operationId 중복 정리 등)가 함께 적용됐다.
	// 그래서 기본 리소스의 customizer 중 product가 등록한 것(app 패키지)만 빼고 나머지는 그대로 이어 받는다.
	private fun adminCustomizers(defaults: SpringDocCustomizers): SpringDocCustomizers {
		val openApiCustomizers: Set<OpenApiCustomizer> = linkedSetOf<OpenApiCustomizer>(AdminPathKeyCustomizer()).apply {
			addAll(ADMIN_OPEN_API_CUSTOMIZERS)
			addAll(frameworkOnly(defaults.openApiCustomizers))
		}
		val operationCustomizers: Set<OperationCustomizer> = linkedSetOf<OperationCustomizer>().apply {
			addAll(ADMIN_OPERATION_CUSTOMIZERS)
			addAll(frameworkOnly(defaults.operationCustomizers))
		}
		val methodFilters: Set<OpenApiMethodFilter> = linkedSetOf<OpenApiMethodFilter>(OpenApiMethodFilter { method -> AdminApi.isAdminController(method.declaringClass) }).apply {
			addAll(frameworkOnly(defaults.methodFilters))
		}
		return SpringDocCustomizers(
			Optional.of(openApiCustomizers),
			Optional.of(operationCustomizers),
			defaults.routerOperationCustomizers,
			Optional.of(methodFilters),
			defaults.optionalDelegatingMethodParameterCustomizers,
			defaults.parameterCustomizers
		)
	}

	private fun <T : Any> frameworkOnly(beans: Optional<out Collection<T>>): List<T> = beans.map { all -> all.filterNot { isApplicationBean(it) } }.orElse(emptyList())

	private fun isApplicationBean(bean: Any): Boolean = bean.javaClass.name.startsWith(APPLICATION_PACKAGE_PREFIX)

	// admin 앱의 문서는 context-path(/admin/api)를 servers에 두고 paths 키는 /v1/..., /v2/... 형태였다.
	// 통합 앱의 handler 경로에는 /admin/api가 붙어 있으므로 키에서 떼어 같은 모양으로 되돌린다. 다른 customizer보다 먼저 실행한다.
	internal class AdminPathKeyCustomizer : OpenApiCustomizer {
		override fun customise(openApi: OpenAPI) {
			val paths = openApi.paths ?: return
			val rewritten = Paths()
			paths.forEach { (path, item) -> rewritten.addPathItem(path.removePrefix(AdminApi.PATH_PREFIX), item) }
			rewritten.extensions = paths.extensions
			openApi.paths = rewritten
		}
	}

	companion object {
		private const val GROUP_NAME = "admin"

		// product가 bean으로 등록한 문서 customizer와 method filter는 product 문서 전용이다.
		private const val APPLICATION_PACKAGE_PREFIX = "app."

		// admin 앱의 springdoc.api-docs.path. context-path(/admin/api)와 합쳐 AdminApi.DOCS_PATH가 된다.
		const val ADMIN_APP_API_DOCS_PATH = "/v1/openapi.admin.json"

		private val COMMON_ERROR_RESPONSE_CUSTOMIZER = AdminCommonErrorResponseCustomizer()

		// admin 앱에서 bean으로 등록되던 문서 customizer 전체
		val ADMIN_OPEN_API_CUSTOMIZERS: List<OpenApiCustomizer> = listOf(
			COMMON_ERROR_RESPONSE_CUSTOMIZER,
			AlcoholAttributeSchemaCustomizer(),
			AlcoholScenarioSchemaCustomizer(),
			AdminMfdsNameFieldSchemaCustomizer()
		)

		val ADMIN_OPERATION_CUSTOMIZERS: List<OperationCustomizer> = listOf(
			COMMON_ERROR_RESPONSE_CUSTOMIZER,
			AdminGlobalResponseSchemaCustomizer(),
			AdminSecurityRequirementCustomizer()
		)
	}
}
