package app.global.common

import app.external.version.config.AppInfoConfig
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
class ApplicationReadyEvent(
	private val info: AppInfoConfig,
) {

	@Order(Int.MAX_VALUE - 1)
	@EventListener(ApplicationReadyEvent::class)
	fun onApplicationReady() {
		println("========================================")
		println("Server Name: ${info.serverName}")
		println("Environment: ${info.environment}")
		println("Git Branch: ${info.gitBranch}")
		println("Git Commit: ${info.gitCommit}")
		println("Git Build Time: ${info.gitBuildTime}")
		println("========================================")
	}
}
