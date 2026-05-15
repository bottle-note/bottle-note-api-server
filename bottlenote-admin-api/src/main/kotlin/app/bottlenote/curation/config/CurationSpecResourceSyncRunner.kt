package app.bottlenote.curation.config

import app.bottlenote.curation.service.CurationSpecResourceSyncService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
	prefix = "curation.spec-sync",
	name = ["enabled"],
	havingValue = "true",
	matchIfMissing = true
)
class CurationSpecResourceSyncRunner(
	private val curationSpecResourceSyncService: CurationSpecResourceSyncService
) {
	private val log = LoggerFactory.getLogger(javaClass)

	@EventListener(ApplicationReadyEvent::class)
	fun sync() {
		val result = curationSpecResourceSyncService.sync()
		log.info(
			"큐레이션 스펙 리소스 동기화 완료: created={}, updated={}, total={}",
			result.createdCount(),
			result.updatedCount(),
			result.totalCount()
		)
	}
}
