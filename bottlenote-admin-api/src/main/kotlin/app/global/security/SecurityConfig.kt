package app.global.security

import app.bottlenote.global.security.accesscontrol.AccessControlFilter
import app.bottlenote.global.security.constant.MaliciousPathPattern
import app.bottlenote.global.security.jwt.AdminJwtAuthenticationFilter
import app.bottlenote.global.security.jwt.AdminJwtAuthenticationManager
import app.bottlenote.global.security.policy.SecurityPolicyRegistry
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.CorsUtils
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
	private val adminJwtAuthenticationManager: AdminJwtAuthenticationManager,
	private val securityPolicyRegistry: SecurityPolicyRegistry,
	private val corsProperties: CorsProperties,
	private val accessControlFilterProvider: ObjectProvider<AccessControlFilter>
) {
	@Bean
	fun filterChain(http: HttpSecurity, corsConfigurationSource: CorsConfigurationSource): SecurityFilterChain {
		http
			.csrf { it.disable() }
			.cors { it.configurationSource(corsConfigurationSource) }
			.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
			.formLogin { it.disable() }
			.httpBasic { it.disable() }
			.authorizeHttpRequests { auth ->
				auth
					.requestMatchers(MaliciousPathPattern::matches)
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
				AdminJwtAuthenticationFilter(adminJwtAuthenticationManager, securityPolicyRegistry),
				UsernamePasswordAuthenticationFilter::class.java
			)

		accessControlFilterProvider.ifAvailable { filter ->
			http.addFilterBefore(filter, AdminJwtAuthenticationFilter::class.java)
		}

		return http.build()
	}

	// 문서 스펙 경로는 일반 API와 분리된 CORS 정책을 쓴다.
	@Bean
	fun corsConfigurationSource(
		@Value("\${springdoc.api-docs.path}") openApiSpecPath: String
	): CorsConfigurationSource {
		val docsConfiguration = CorsConfiguration()
		docsConfiguration.allowedOrigins = corsProperties.docsAllowedOrigins
		docsConfiguration.allowedMethods = listOf("GET", "OPTIONS")
		docsConfiguration.allowedHeaders = listOf("Authorization", "Content-Type")
		docsConfiguration.allowCredentials = false

		val configuration = CorsConfiguration()
		configuration.allowedOrigins = corsProperties.allowedOrigins
		configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
		configuration.allowedHeaders = listOf("Authorization", "Content-Type")
		configuration.allowCredentials = false

		val source = UrlBasedCorsConfigurationSource()
		source.registerCorsConfiguration(openApiSpecPath, docsConfiguration)
		source.registerCorsConfiguration("/**", configuration)
		return source
	}

	@Bean
	fun bCryptPasswordEncoder(): BCryptPasswordEncoder = BCryptPasswordEncoder()
}
