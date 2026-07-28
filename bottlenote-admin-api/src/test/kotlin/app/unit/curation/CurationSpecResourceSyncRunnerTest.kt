package app.unit.curation

import app.bottlenote.curation.config.CurationSpecResourceSyncRunner
import app.bottlenote.curation.domain.CurationFeedRegenerationLock
import app.bottlenote.curation.dto.response.CurationSpecSyncResponse
import app.bottlenote.curation.service.CurationFeedPayloadRegenerationService
import app.bottlenote.curation.service.CurationSpecResourceSyncService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("unit")
@DisplayName("CurationSpecResourceSyncRunner 단위 테스트")
class CurationSpecResourceSyncRunnerTest {

	// 재생성 호출 인자를 기록하는 Fake. 실제 추출은 mono 단위 테스트가 검증한다.
	private class RecordingRegenerationService(
		private val onRegenerate: (Collection<Long>) -> Int = { it.size }
	) : CurationFeedPayloadRegenerationService(null, null, null) {
		val calls = mutableListOf<Collection<Long>>()
		val invalidated = mutableListOf<Collection<Long>>()

		override fun invalidate(specIds: Collection<Long>?): Int {
			invalidated.add(specIds ?: emptyList())
			return (specIds ?: emptyList()).size
		}

		override fun regenerate(specIds: Collection<Long>?): Int {
			calls.add(specIds ?: emptyList())
			return onRegenerate(specIds ?: emptyList())
		}
	}

	private class ExplodingLock(private val onAcquire: Boolean) : CurationFeedRegenerationLock {
		override fun tryAcquire(): Boolean = if (onAcquire) throw IllegalStateException("Redis 연결 실패") else true
		override fun release(): Unit = throw IllegalStateException("Redis 연결 실패")
	}

	private class FakeLock(private val acquirable: Boolean) : CurationFeedRegenerationLock {
		var acquireCount = 0
		var releaseCount = 0

		override fun tryAcquire(): Boolean {
			acquireCount++
			return acquirable
		}

		override fun release() {
			releaseCount++
		}
	}

	private fun syncServiceReturning(changedSpecIds: List<Long>) = object : CurationSpecResourceSyncService(null, null, null, null) {
		override fun sync() = CurationSpecSyncResponse(0, 4, changedSpecIds)
	}

	@Test
	@DisplayName("변경된 스펙이 없으면 락을 잡지 않고 재생성도 하지 않는다")
	fun sync_whenNoChangedSpec_skipsRegeneration() {
		val regeneration = RecordingRegenerationService()
		val lock = FakeLock(true)

		CurationSpecResourceSyncRunner(syncServiceReturning(emptyList()), regeneration, lock).sync()

		assertThat(regeneration.calls).isEmpty()
		assertThat(lock.acquireCount).isZero()
	}

	@Test
	@DisplayName("변경된 스펙이 있으면 그 스펙만 재생성하고 락을 해제한다")
	fun sync_whenSpecChanged_regeneratesAndReleasesLock() {
		val regeneration = RecordingRegenerationService()
		val lock = FakeLock(true)

		CurationSpecResourceSyncRunner(syncServiceReturning(listOf(1L, 3L)), regeneration, lock).sync()

		assertThat(regeneration.calls).containsExactly(listOf(1L, 3L))
		assertThat(lock.releaseCount).isEqualTo(1)
	}

	@Test
	@DisplayName("락을 얻지 못하면 재생성을 건너뛰고 해제도 하지 않는다")
	fun sync_whenLockNotAcquired_skipsRegeneration() {
		val regeneration = RecordingRegenerationService()
		val lock = FakeLock(false)

		CurationSpecResourceSyncRunner(syncServiceReturning(listOf(1L)), regeneration, lock).sync()

		assertThat(regeneration.calls).isEmpty()
		assertThat(lock.releaseCount).isZero()
	}

	@Test
	@DisplayName("무효화를 먼저 수행한 뒤 재생성한다")
	fun sync_invalidatesBeforeRegenerating() {
		val regeneration = RecordingRegenerationService()
		val lock = FakeLock(true)

		CurationSpecResourceSyncRunner(syncServiceReturning(listOf(1L)), regeneration, lock).sync()

		assertThat(regeneration.invalidated).containsExactly(listOf(1L))
		assertThat(regeneration.calls).containsExactly(listOf(1L))
	}

	@Test
	@DisplayName("락 획득 중 Redis가 죽어도 기동을 막지 않는다")
	fun sync_whenLockAcquireThrows_doesNotBlockStartup() {
		val runner = CurationSpecResourceSyncRunner(
			syncServiceReturning(listOf(1L)),
			RecordingRegenerationService(),
			ExplodingLock(onAcquire = true)
		)

		assertThatCode { runner.sync() }.doesNotThrowAnyException()
	}

	@Test
	@DisplayName("락 해제 중 Redis가 죽어도 기동을 막지 않는다")
	fun sync_whenLockReleaseThrows_doesNotBlockStartup() {
		val runner = CurationSpecResourceSyncRunner(
			syncServiceReturning(listOf(1L)),
			RecordingRegenerationService(),
			ExplodingLock(onAcquire = false)
		)

		assertThatCode { runner.sync() }.doesNotThrowAnyException()
	}

	@Test
	@DisplayName("재생성이 실패해도 기동을 막지 않고 락은 해제한다")
	fun sync_whenRegenerationFails_doesNotBlockStartup() {
		val regeneration = RecordingRegenerationService { throw IllegalStateException("추출 실패") }
		val lock = FakeLock(true)
		val runner =
			CurationSpecResourceSyncRunner(syncServiceReturning(listOf(1L)), regeneration, lock)

		assertThatCode { runner.sync() }.doesNotThrowAnyException()
		assertThat(lock.releaseCount).isEqualTo(1)
	}
}
