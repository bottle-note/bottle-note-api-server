package app.global.common

import app.external.version.config.AppInfoConfig
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
class ApplicationReadyEvent(
	private val info: AppInfoConfig
) {
	companion object {
		private val log = LoggerFactory.getLogger(ApplicationReadyEvent::class.java)
	}

	@Order(Int.MAX_VALUE - 1)
	@EventListener(ApplicationReadyEvent::class)
	fun displayServiceBanner() {
		val banner =
			"""
				▗▄▄▖  ▗▄▖▗▄▄▄▖▗▄▄▄▖▗▖   ▗▄▄▄▖    ▗▖  ▗▖ ▗▄▖▗▄▄▄▖▗▄▄▄▖
				▐▌ ▐▌▐▌ ▐▌ █    █  ▐▌   ▐▌       ▐▛▚▖▐▌▐▌ ▐▌ █  ▐▌
				▐▛▀▚▖▐▌ ▐▌ █    █  ▐▌   ▐▛▀▀▘    ▐▌ ▝▜▌▐▌ ▐▌ █  ▐▛▀▀▘
				▐▙▄▞▘▝▚▄▞▘ █    █  ▐▙▄▄▖▐▙▄▄▖    ▐▌  ▐▌▝▚▄▞▘ █  ▐▙▄▄▖
				 ▗▄▖ ▗▄▄▄ ▗▖  ▗▖▗▄▄▄▖▗▖  ▗▖     ▗▄▖ ▗▄▄▖▗▄▄▄▖
				▐▌ ▐▌▐▌  █▐▛▚▞▜▌  █  ▐▛▚▖▐▌    ▐▌ ▐▌▐▌ ▐▌ █
				▐▛▀▜▌▐▌  █▐▌  ▐▌  █  ▐▌ ▝▜▌    ▐▛▀▜▌▐▛▀▘  █
				▐▌ ▐▌▐▙▄▄▀▐▌  ▐▌▗▄█▄▖▐▌  ▐▌    ▐▌ ▐▌▐▌  ▗▄█▄▖
			================================================================================
			  보틀노트 Admin API 서버가 성공적으로 시작되었습니다.
			  - Server Name  : ${info.serverName}
			  - Environment  : ${info.environment}
			  - Git Branch   : ${info.gitBranch}
			  - Git Commit   : ${info.gitCommit}
			  - Build Time   : ${info.gitBuildTime}
			================================================================================
			""".trimIndent()
		println(banner)
	}
}
