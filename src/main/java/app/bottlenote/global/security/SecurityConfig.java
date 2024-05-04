package app.bottlenote.global.security;

import app.bottlenote.common.jwt.JwtAuthenticationFilter;
import app.bottlenote.common.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
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

	private final SocialLoginAuthenticationProvider socialLoginAuthenticationProvider;
	private final JwtTokenProvider jwtTokenProvider;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.csrf(AbstractHttpConfigurer::disable)
			.authenticationProvider(socialLoginAuthenticationProvider)
			.sessionManagement(sessionConfig -> sessionConfig.sessionCreationPolicy(STATELESS))
			.formLogin(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/api/v1/oauth/**").permitAll()
				.requestMatchers("/system/**").permitAll()
				.anyRequest().permitAll()
			)
			.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
				UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

		// 기본 CORS 설정
		CorsConfiguration defaultConfiguration = new CorsConfiguration();
		defaultConfiguration.setAllowedOrigins(Arrays.asList("http://bottle-note.com", "http://localhost:3000"));
		defaultConfiguration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
		source.registerCorsConfiguration("/api/**", defaultConfiguration);

		// /system/actuator/** 경로에 대한 특정 CORS 설정
		CorsConfiguration actuatorConfiguration = new CorsConfiguration();
		actuatorConfiguration.setAllowedOrigins(List.of("*"));
		actuatorConfiguration.setAllowedMethods(List.of("GET"));
		source.registerCorsConfiguration("/system/actuator/**", actuatorConfiguration);

		return source;
	}

	@Bean
	public AuthenticationManager authenticationManager(
		AuthenticationConfiguration authenticationConfiguration)
		throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}
}
