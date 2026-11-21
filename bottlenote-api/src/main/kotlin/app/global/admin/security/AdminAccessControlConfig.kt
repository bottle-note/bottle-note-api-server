package app.global.admin.security

import app.bottlenote.global.security.accesscontrol.AccessControlFilter
import app.bottlenote.global.security.accesscontrol.AccessControlMetrics
import app.bottlenote.global.security.accesscontrol.AccessControlProperties
import app.bottlenote.global.security.accesscontrol.AccessControlService
import app.bottlenote.global.security.accesscontrol.AccessControlStore
import app.bottlenote.global.security.accesscontrol.BanSnapshotHolder
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

// 기존 admin 앱의 access-control 설정(버킷 네임스페이스, 기본·unknown-IP 한도, 경로 규칙, 관리 평면 예외)을 admin 체인에 그대로 적용한다.
// 저장소, 지표, ban snapshot은 product와 공유한다. ban 키는 원래 네임스페이스 없이 두 앱이 공유했고 rate-limit 키는 네임스페이스로 나뉜다.
// 켜고 끄는 기준은 저장소를 만드는 bottlenote.access-control.enabled 하나다.
@Configuration
@ConditionalOnProperty(
	prefix = "bottlenote.access-control",
	name = ["enabled"],
	havingValue = "true",
	matchIfMissing = true
)
class AdminAccessControlConfig {
	// product의 AccessControlProperties 주입에 섞이지 않도록 autowire 후보에서 뺀다
	@Bean(autowireCandidate = false)
	@ConfigurationProperties(prefix = PROPERTIES_PREFIX)
	fun adminAccessControlProperties(): AccessControlProperties = AccessControlProperties()

	@Bean
	fun adminAccessControlFilter(
		accessControlStore: AccessControlStore,
		accessControlMetrics: AccessControlMetrics,
		banSnapshotHolder: BanSnapshotHolder,
		objectMapper: ObjectMapper
	): AdminAccessControlFilter {
		val service =
			AccessControlService(
				accessControlStore,
				adminAccessControlProperties(),
				accessControlMetrics,
				banSnapshotHolder,
				Clock.systemUTC()
			)
		return AdminAccessControlFilter(AccessControlFilter(service, objectMapper))
	}

	// 서블릿 컨테이너 자동 등록을 막는다. admin 보안 체인 안에서만 동작한다.
	@Bean
	fun adminAccessControlFilterRegistration(filter: AdminAccessControlFilter): FilterRegistrationBean<AdminAccessControlFilter> {
		val registration = FilterRegistrationBean(filter)
		registration.isEnabled = false
		return registration
	}

	companion object {
		const val PROPERTIES_PREFIX = "bottlenote.admin-access-control"
	}
}
