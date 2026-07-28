package app.bottlenote.curation.config

import app.bottlenote.curation.domain.CurationFeedRegenerationLock
import app.bottlenote.curation.service.CurationFeedPayloadRegenerationService
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
	private val curationSpecResourceSyncService: CurationSpecResourceSyncService,
	private val curationFeedPayloadRegenerationService: CurationFeedPayloadRegenerationService,
	private val curationFeedRegenerationLock: CurationFeedRegenerationLock
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
		regenerateFeedPayloads(result.changedSpecIds())
	}

	// feed_payload는 파생 데이터고 NULL fallback이 있다. 재생성 실패나 Redis 장애로 기동을 막지 않는다.
	private fun regenerateFeedPayloads(changedSpecIds: List<Long>) {
		if (changedSpecIds.isEmpty()) {
			return
		}
		var acquired = false
		try {
			acquired = curationFeedRegenerationLock.tryAcquire()
			if (!acquired) {
				log.info("다른 인스턴스가 재생성 중이라 건너뜁니다: specIds={}", changedSpecIds)
				return
			}
			// 무효화를 먼저 커밋한다. 뒤 단계가 실패해도 낡은 값이 아니라 NULL이 남아 원본으로 fallback된다.
			curationFeedPayloadRegenerationService.invalidate(changedSpecIds)
			val regenerated = curationFeedPayloadRegenerationService.regenerate(changedSpecIds)
			log.info(
				"responseSpec 변경으로 feed_payload 재생성: specIds={}, curations={}",
				changedSpecIds,
				regenerated
			)
		} catch (e: Exception) {
			log.warn(
				"feed_payload 재생성에 실패했습니다. 조회는 원본 payload로 대체됩니다: specIds={}",
				changedSpecIds,
				e
			)
		} finally {
			if (acquired) {
				runCatching { curationFeedRegenerationLock.release() }
					.onFailure { log.warn("재생성 락 해제에 실패했습니다. TTL로 만료됩니다.", it) }
			}
		}
	}
}
