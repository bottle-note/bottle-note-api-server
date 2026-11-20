package app.bottlenote.curation.config

import app.bottlenote.curation.dto.response.CurationSpecSyncResponse
import app.bottlenote.curation.service.CurationFeedPayloadRefreshService
import app.bottlenote.curation.service.CurationSpecResourceSyncService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.dao.ConcurrencyFailureException
import org.springframework.dao.DataIntegrityViolationException
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
	private val curationFeedPayloadRefreshService: CurationFeedPayloadRefreshService
) {
	private val log = LoggerFactory.getLogger(javaClass)

	@EventListener(ApplicationReadyEvent::class)
	fun sync() {
		val result = syncTolerantOfConcurrentStartup()
		log.info(
			"큐레이션 스펙 리소스 동기화 완료: created={}, updated={}, total={}",
			result.createdCount(),
			result.updatedCount(),
			result.totalCount()
		)
		curationFeedPayloadRefreshService.refresh(result.changedSpecIds())
	}

	// product의 모든 레플리카가 기동 시 이 작업을 실행한다. 동시에 기동하면 같은 code를 함께 insert해 uk_curation_spec_code 충돌이 날 수 있다.
	// 충돌한 쪽은 트랜잭션 전체가 롤백되므로 다시 실행하면 먼저 커밋된 행을 찾아 update 경로로 수렴한다.
	// 스펙 검증 실패 같은 결정적 오류는 재시도하지 않고 기존처럼 기동을 실패시킨다.
	private fun syncTolerantOfConcurrentStartup(): CurationSpecSyncResponse = retryOnConcurrentConflict(MAX_ATTEMPTS, { attempt, e ->
		log.warn("다른 인스턴스와 큐레이션 스펙 동기화가 겹쳐 다시 시도합니다: attempt={}, cause={}", attempt, e.javaClass.simpleName)
		Thread.sleep(RETRY_BACKOFF_MILLIS * attempt)
	}) { curationSpecResourceSyncService.sync() }

	companion object {
		private const val MAX_ATTEMPTS = 3
		private const val RETRY_BACKOFF_MILLIS = 200L
	}
}

// 동시 실행 충돌(유니크 제약 위반, 잠금 충돌)만 다시 시도한다. 마지막 시도의 예외는 그대로 던진다.
internal fun <T> retryOnConcurrentConflict(
	maxAttempts: Int,
	beforeRetry: (attempt: Int, cause: RuntimeException) -> Unit,
	action: () -> T
): T {
	require(maxAttempts >= 1) { "maxAttempts는 1 이상이어야 합니다." }
	var attempt = 1
	while (true) {
		try {
			return action()
		} catch (e: RuntimeException) {
			val conflict = e is DataIntegrityViolationException || e is ConcurrencyFailureException
			if (!conflict || attempt >= maxAttempts) {
				throw e
			}
			beforeRetry(attempt, e)
			attempt++
		}
	}
}
