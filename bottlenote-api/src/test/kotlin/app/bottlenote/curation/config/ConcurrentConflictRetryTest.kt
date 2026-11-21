package app.bottlenote.curation.config

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.dao.CannotAcquireLockException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.DuplicateKeyException

@Tag("unit")
@DisplayName("[unit] 큐레이션 스펙 동기화의 동시 실행 충돌 재시도")
class ConcurrentConflictRetryTest {

	@Test
	@DisplayName("유니크 제약 충돌이 난 뒤 다시 시도할 때 성공 결과를 돌려준다")
	fun retriesAfterUniqueConstraintConflict() {
		var calls = 0
		val retries = mutableListOf<Int>()

		val result = retryOnConcurrentConflict(3, { attempt, _ -> retries += attempt }) {
			calls++
			if (calls == 1) throw DuplicateKeyException("uk_curation_spec_code")
			"synced"
		}

		assertThat(result).isEqualTo("synced")
		assertThat(calls).isEqualTo(2)
		assertThat(retries).containsExactly(1)
	}

	@Test
	@DisplayName("잠금 충돌이 난 뒤 다시 시도할 때 성공 결과를 돌려준다")
	fun retriesAfterLockConflict() {
		var calls = 0

		val result = retryOnConcurrentConflict(3, { _, _ -> }) {
			calls++
			if (calls < 3) throw CannotAcquireLockException("deadlock")
			calls
		}

		assertThat(result).isEqualTo(3)
	}

	@Test
	@DisplayName("최대 횟수까지 충돌이 이어질 때 마지막 예외를 그대로 던진다")
	fun rethrowsLastConflictWhenAttemptsRunOut() {
		var calls = 0

		assertThatThrownBy {
			retryOnConcurrentConflict(3, { _, _ -> }) {
				calls++
				throw DataIntegrityViolationException("conflict-$calls")
			}
		}.isInstanceOf(DataIntegrityViolationException::class.java).hasMessage("conflict-3")
		assertThat(calls).isEqualTo(3)
	}

	@Test
	@DisplayName("스펙 검증 실패처럼 충돌이 아닌 예외가 날 때 다시 시도하지 않는다")
	fun doesNotRetryNonConflictFailures() {
		var calls = 0

		assertThatThrownBy {
			retryOnConcurrentConflict(3, { _, _ -> }) {
				calls++
				throw IllegalStateException("invalid spec")
			}
		}.isInstanceOf(IllegalStateException::class.java)
		assertThat(calls).isEqualTo(1)
	}
}
