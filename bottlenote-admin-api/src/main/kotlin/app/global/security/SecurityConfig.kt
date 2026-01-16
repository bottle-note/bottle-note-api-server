package app.global.security

import app.bottlenote.global.security.constant.MaliciousPathPattern
import app.bottlenote.global.security.jwt.AdminJwtAuthenticationFilter
import app.bottlenote.global.security.jwt.AdminJwtAuthenticationManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
	private val adminJwtAuthenticationManager: AdminJwtAuthenticationManager
) {

	@Bean
	fun filterChain(http: HttpSecurity): SecurityFilterChain {
		return http
			.csrf { it.disable() }
			.cors { it.configurationSource(corsConfigurationSource()) }
			.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
			.formLogin { it.disable() }
			.httpBasic { it.disable() }
			.authorizeHttpRequests { auth ->
				auth
					.requestMatchers(*MaliciousPathPattern.getAllPatterns()).denyAll()
					.requestMatchers("/auth/login", "/auth/refresh").permitAll()
					.requestMatchers("/actuator/**").permitAll()
					.anyRequest().authenticated()
			}
			.addFilterBefore(
				AdminJwtAuthenticationFilter(adminJwtAuthenticationManager),
				UsernamePasswordAuthenticationFilter::class.java
			)
			.build()
	}

	@Bean
	fun corsConfigurationSource(): CorsConfigurationSource {
		val configuration = CorsConfiguration()
		configuration.allowedOrigins = listOf("*")
		configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
		configuration.allowedHeaders = listOf("*")
		configuration.allowCredentials = false

		val source = UrlBasedCorsConfigurationSource()
		source.registerCorsConfiguration("/**", configuration)
		return source
	}

	@Bean
	fun bCryptPasswordEncoder(): BCryptPasswordEncoder {
		return BCryptPasswordEncoder()
	}
}
