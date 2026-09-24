package app.bottlenote.mfds.presentation

import app.bottlenote.global.annotation.SecurityPolicy
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("unit")
@DisplayName("MFDS 일괄 매칭은 공개 예외 없이 관리자 인증을 요구한다")
class AdminMfdsBulkMatchingSecurityTest {
	@Test
	@DisplayName("미리보기와 확정에 PUBLIC 정책을 붙이지 않는다")
	fun bulkEndpointsDoNotOptOutOfAdminAuth() {
		val controller = AdminMfdsBulkMatchingController::class.java
		assertThat(controller.getAnnotation(SecurityPolicy::class.java)).isNull()
		assertThat(controller.declaredMethods.map { it.name })
			.contains("preview", "confirm")
		controller.declaredMethods
			.filter { it.name == "preview" || it.name == "confirm" }
			.forEach { method ->
				assertThat(method.getAnnotation(SecurityPolicy::class.java)).isNull()
			}
	}
}
