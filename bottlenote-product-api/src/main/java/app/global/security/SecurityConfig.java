package app.global.security;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import app.bottlenote.global.security.constant.MaliciousPathPattern;
import app.bottlenote.global.security.jwt.JwtAuthenticationEntryPoint;
import app.bottlenote.global.security.jwt.JwtAuthenticationFilter;
import app.bottlenote.global.security.jwt.JwtAuthenticationManager;
import app.bottlenote.global.security.policy.SecurityPolicyRegistry;
import app.bottlenote.observability.visitor.VisitorTelemetryFilter;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableConfigurationProperties(CorsProperties.class)
public class SecurityConfig {

  private final JwtAuthenticationManager jwtAuthenticationManager;
  private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
  private final SecurityPolicyRegistry securityPolicyRegistry;
  private final ObjectProvider<VisitorTelemetryFilter> visitorTelemetryFilterProvider;
  private final CorsProperties corsProperties;

  /**
   * 세션 메서드 참조를 위한 참조 메서드
   *
   * @param sessionConfig the session config
   */
  private static void statelessSessionConfig(
      SessionManagementConfigurer<HttpSecurity> sessionConfig) {
    sessionConfig.sessionCreationPolicy(STATELESS);
  }

  @PostConstruct
  public void setup() {
    SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
  }

  private static boolean hasJwtException(HttpServletRequest request) {
    return request.getAttribute("exception") != null;
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
    http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(SecurityConfig::statelessSessionConfig)
        .formLogin(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(MaliciousPathPattern::matches)
                    .denyAll()
                    .requestMatchers(CorsUtils::isPreFlightRequest)
                    .permitAll()
                    .requestMatchers(SecurityConfig::hasJwtException)
                    .fullyAuthenticated()
                    .requestMatchers(securityPolicyRegistry::requiresAuthentication)
                    .fullyAuthenticated()
                    .anyRequest()
                    .permitAll())
        .addFilterBefore(
            new JwtAuthenticationFilter(jwtAuthenticationManager, securityPolicyRegistry),
            UsernamePasswordAuthenticationFilter.class);

    visitorTelemetryFilterProvider.ifAvailable(
        filter -> http.addFilterAfter(filter, JwtAuthenticationFilter.class));

    return http.exceptionHandling(
            exceptionHandling ->
                exceptionHandling.authenticationEntryPoint(jwtAuthenticationEntryPoint))
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
    configuration.setAllowedOrigins(corsProperties.allowedOrigins());
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    configuration.setAllowCredentials(false);
    source.registerCorsConfiguration("/**", configuration);

    return source;
  }

  /** BCryptPasswordEncoder 빈 등록 */
  @Bean
  BCryptPasswordEncoder bCryptPasswordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
