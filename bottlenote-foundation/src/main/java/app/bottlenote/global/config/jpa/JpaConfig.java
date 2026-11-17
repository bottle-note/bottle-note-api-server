package app.bottlenote.global.config.jpa;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** JPA 전역 설정 클래스. 감사자(AuditorAware)는 user 쪽 bean을 타입으로 찾아 쓴다. */
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "app")
@Configuration
public class JpaConfig {}
