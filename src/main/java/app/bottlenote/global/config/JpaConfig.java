package app.bottlenote.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA 전역 설정 클래스
 */
@EnableJpaAuditing
@Configuration
public class JpaConfig {

}

