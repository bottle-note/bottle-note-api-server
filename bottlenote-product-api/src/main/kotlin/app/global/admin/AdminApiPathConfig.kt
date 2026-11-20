package app.global.admin

import jakarta.persistence.EntityManagerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

// admin 앱의 context-path(/admin/api)와 AdminApiVersionConfig(/v1)를 합친 외부 URL을 그대로 유지한다.
@Configuration
class AdminApiPathConfig(
	private val entityManagerFactory: EntityManagerFactory
) : WebMvcConfigurer {

	// addPathPrefix는 처음 일치한 prefix 하나만 적용하므로 두 판정은 서로 겹치지 않게 둔다.
	// product 컨트롤러는 어느 판정에도 걸리지 않아 prefix가 붙지 않는다.
	override fun configurePathMatch(configurer: PathMatchConfigurer) {
		configurer.addPathPrefix(AdminApi.LEGACY_PATH_PREFIX) { AdminApi.isLegacyController(it) }
		configurer.addPathPrefix(AdminApi.PATH_PREFIX) { AdminApi.isV2Controller(it) }
	}

	// admin 앱은 spring.jpa.open-in-view가 기본값(true)으로 동작했다. product 경로는 계속 끄고 admin 경로에서만 같은 동작을 유지한다.
	override fun addInterceptors(registry: InterceptorRegistry) {
		val interceptor = OpenEntityManagerInViewInterceptor()
		interceptor.entityManagerFactory = entityManagerFactory
		registry.addWebRequestInterceptor(interceptor).addPathPatterns("${AdminApi.PATH_PREFIX}/**")
	}
}
