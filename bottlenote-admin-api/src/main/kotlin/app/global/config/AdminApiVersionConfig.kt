package app.global.config

import app.bottlenote.curation.presentation.AdminCurationSpecController
import app.bottlenote.curation.presentation.AdminSpecBasedCurationController
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class AdminApiVersionConfig : WebMvcConfigurer {
	override fun configurePathMatch(configurer: PathMatchConfigurer) {
		configurer.addPathPrefix("/v1") { controllerType ->
			isLegacyAdminController(controllerType)
		}
	}

	private fun isLegacyAdminController(controllerType: Class<*>): Boolean = controllerType.packageName.startsWith("app.bottlenote") &&
		controllerType.packageName.contains(".presentation") &&
		controllerType.simpleName.endsWith("Controller") &&
		!excludedControllers.contains(controllerType)

	private companion object {
		private val excludedControllers = setOf(
			AdminCurationSpecController::class.java,
			AdminSpecBasedCurationController::class.java
		)
	}
}
