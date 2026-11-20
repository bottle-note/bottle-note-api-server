package app.global.admin.security

import app.bottlenote.global.security.constant.MaliciousPathPattern
import app.bottlenote.global.security.jwt.AdminJwtAuthenticationFilter
import app.bottlenote.global.security.jwt.AdminJwtAuthenticationManager
import app.bottlenote.global.security.policy.SecurityPolicyRegistry
import app.global.admin.AdminApi
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.CorsUtils
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

// admin 앱의 SecurityConfig를 /admin/api/** 전용 체인으로 옮겼다.
// 필터 구성, 인가 규칙, CORS, httpBasic, 예외 처리(entry point를 지정하지 않음)는 admin 앱과 같게 유지한다.
// product 체인(any-request)보다 먼저 평가되어야 일반 사용자 토큰이 admin API에 닿지 않는다.
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(AdminCorsProperties::class)
class AdminSecurityConfig(
	private val adminJwtAuthenticationManager: AdminJwtAuthenticationManager,
	private val securityPolicyRegistry: SecurityPolicyRegistry,
	private val adminCorsProperties: AdminCorsProperties,
	private val adminAccessControlFilterProvider: ObjectProvider<AdminAccessControlFilter>
) {
	@Bean
	@Order(ADMIN_FILTER_CHAIN_ORDER)
	fun adminFilterChain(http: HttpSecurity): SecurityFilterChain {
		// 악성 경로는 기존 admin 앱처럼 /admin/api를 context-path로 보고 그 안쪽 경로로 판정한다.
		// 인가 규칙과 JWT 필터가 같은 기준을 써야 공개 경로의 토큰 처리가 admin 앱과 같다.
		val maliciousPath = RequestMatcher { request -> MaliciousPathPattern.matchesUnder(request, AdminApi.PATH_PREFIX) }
		http
			.securityMatcher(AdminApiRequestMatcher)
			.csrf { it.disable() }
			.cors { it.configurationSource(adminCorsConfigurationSource()) }
			.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
			.formLogin { it.disable() }
			.httpBasic { it.disable() }
			.authorizeHttpRequests { auth ->
				auth
					.requestMatchers(maliciousPath)
					.denyAll()
					.requestMatchers(CorsUtils::isPreFlightRequest)
					.permitAll()
					.requestMatchers(RequestMatcher { request -> request.getAttribute("exception") != null })
					.authenticated()
					.requestMatchers(securityPolicyRegistry::requiresAuthentication)
					.authenticated()
					.anyRequest()
					.permitAll()
			}.addFilterBefore(
				AdminJwtAuthenticationFilter(adminJwtAuthenticationManager, securityPolicyRegistry, maliciousPath),
				UsernamePasswordAuthenticationFilter::class.java
			)

		// product 체인의 접근 제어 필터가 아니라 기존 admin 앱 설정으로 만든 필터를 쓴다
		adminAccessControlFilterProvider.ifAvailable { filter ->
			http.addFilterBefore(filter, AdminJwtAuthenticationFilter::class.java)
		}

		return http.build()
	}

	// 문서 스펙 경로는 일반 API와 분리된 CORS 정책을 쓴다. 이 체인은 admin 요청만 받으므로 나머지는 /**로 둔다.
	// bean으로 등록하지 않는다. product 체인의 corsConfigurationSource와 타입 주입이 섞이지 않게 하기 위해서다.
	private fun adminCorsConfigurationSource(): CorsConfigurationSource {
		val docsConfiguration = CorsConfiguration()
		docsConfiguration.allowedOrigins = adminCorsProperties.docsAllowedOrigins
		docsConfiguration.allowedMethods = listOf("GET", "OPTIONS")
		docsConfiguration.allowedHeaders = listOf("Authorization", "Content-Type")
		docsConfiguration.allowCredentials = false

		val configuration = CorsConfiguration()
		configuration.allowedOrigins = adminCorsProperties.allowedOrigins
		configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
		configuration.allowedHeaders = listOf("Authorization", "Content-Type")
		configuration.allowCredentials = false

		val source = UrlBasedCorsConfigurationSource()
		source.registerCorsConfiguration(AdminApi.DOCS_PATH, docsConfiguration)
		source.registerCorsConfiguration("/**", configuration)
		return source
	}

	companion object {
		const val ADMIN_FILTER_CHAIN_ORDER = 1
	}
}
