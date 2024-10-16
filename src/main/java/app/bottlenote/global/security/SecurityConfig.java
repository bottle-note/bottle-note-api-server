package app.bottlenote.global.security;

import app.bottlenote.global.security.jwt.JwtAuthenticationEntryPoint;
import app.bottlenote.global.security.jwt.JwtAuthenticationFilter;
import app.bottlenote.global.security.jwt.JwtAuthenticationManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationManager jwtAuthenticationManager;
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

	/**
	 * 세션 메서드 참조를 위한 참조 메서드
	 *
	 * @param sessionConfig the session config
	 */
	private static void statelessSessionConfig(SessionManagementConfigurer<HttpSecurity> sessionConfig) {
		sessionConfig.sessionCreationPolicy(STATELESS);
	}

	/**
	 * 필터 체인 보안 설정
	 *
	 * @param http the http
	 * @return the security filter chain
	 * @throws Exception the exception
	 */
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		return http
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.csrf(AbstractHttpConfigurer::disable)
			.sessionManagement(SecurityConfig::statelessSessionConfig)
			.formLogin(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/api/v1/picks/**").authenticated()
				.requestMatchers("/api/v1/s3/**").authenticated()
				.requestMatchers("/api/v1/follow").authenticated()
				.requestMatchers("/api/v1/reviews/me/**").authenticated()
				.requestMatchers(HttpMethod.GET, "api/v1/reviews/**").permitAll()
				.requestMatchers("/api/v1/reviews/**").authenticated()
				.requestMatchers("/api/v1/users/**").authenticated()
				.requestMatchers("/api/v1/help/**").authenticated()
				.requestMatchers("/api/v1/my-page/**").permitAll()
				.anyRequest().permitAll()
			)
			.addFilterBefore(new JwtAuthenticationFilter(jwtAuthenticationManager), UsernamePasswordAuthenticationFilter.class)
			.exceptionHandling(exceptionHandling -> exceptionHandling.authenticationEntryPoint(jwtAuthenticationEntryPoint))
			.build();
	}

	/**
	 * Cors 구성 소스 빈 등록
	 *
	 * @return the cors configuration source
	 */
	@Bean
	CorsConfigurationSource corsConfigurationSource() {

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(List.of("*")); // 모든 origin 허용
		configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("*")); // 모든 헤더 허용
		configuration.setAllowCredentials(false); // credentials 허용하지 않음
		source.registerCorsConfiguration("/**", configuration); // 모든 경로에 대해 설정 적용

		return source;
	}

}
