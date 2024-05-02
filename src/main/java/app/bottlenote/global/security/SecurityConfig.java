package app.bottlenote.global.security;

import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable);
		return http.build();
	}

	//  CORS 설정
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
}
