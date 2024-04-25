package app.bottlenote.global.security;

import app.bottlenote.user.oauth.OAuth2UserService;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	private final OAuth2UserService oAuth2UserService;

	public SecurityConfig(OAuth2UserService oAuth2UserService) {
		this.oAuth2UserService = oAuth2UserService;
	}

//	private final CustomOAuth2UserService customOAuth2UserService;
//
//	public SecurityConfig(CustomOAuth2UserService customOAuth2UserService) {
//		this.customOAuth2UserService = customOAuth2UserService;
//	}

	@Bean
	public BCryptPasswordEncoder encoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http,
		HandlerMappingIntrospector introspector) throws Exception {
		http
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.sessionManagement((sessionManagement) ->
				sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			)
			.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests((authorizeRequests) -> authorizeRequests
				.requestMatchers(new MvcRequestMatcher(introspector, "/api/v1/**")).permitAll()
				.requestMatchers(new MvcRequestMatcher(introspector, "/common/**")).permitAll()
				.requestMatchers(new MvcRequestMatcher(introspector, "/login/**")).permitAll()
				.anyRequest().authenticated()
			)
			.oauth2Login(oauth2Login ->
				oauth2Login.userInfoEndpoint(oauth2Configurer -> oauth2Configurer
						.userService(oAuth2UserService))
					.successHandler(successHandler()));
//		// TODO :: 로그인 성공/실패

		return http.build();
	}

	//  CORS 설정
	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(
			Arrays.asList("http://bottle-note.com", "http://localhost:3000"));
		configuration.setAllowedMethods(
			Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	@Bean
	public AuthenticationSuccessHandler successHandler() {
		return ((request, response, authentication) -> {
			DefaultOAuth2User defaultOAuth2User = (DefaultOAuth2User) authentication.getPrincipal();

			String id = defaultOAuth2User.getAttributes().get("id").toString();
			String body = """
				{"id":"%s"}
				""".formatted(id);

			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			response.setCharacterEncoding(StandardCharsets.UTF_8.name());

			PrintWriter writer = response.getWriter();
			writer.println(body);
			writer.flush();
		});
	}
}
