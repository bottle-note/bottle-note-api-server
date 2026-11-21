package app.global.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/** 비동기 작업에는 제출 시점의 인증만 전달하고, 실행이 끝나면 워커 스레드에서 지운다. */
@Configuration
public class SecurityContextTaskDecoratorConfig {

  @Bean
  public TaskDecorator securityContextTaskDecorator() {
    return runnable -> {
      // 제출 스레드의 컨텍스트가 나중에 바뀌어도 작업에 영향이 없도록 인증만 복사한다
      SecurityContext submitted = SecurityContextHolder.createEmptyContext();
      submitted.setAuthentication(SecurityContextHolder.getContext().getAuthentication());
      return new DelegatingSecurityContextRunnable(runnable, submitted);
    };
  }
}
