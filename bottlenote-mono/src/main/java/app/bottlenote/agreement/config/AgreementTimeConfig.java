package app.bottlenote.agreement.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 약관 동의 기록 시각에 사용할 시간 설정을 제공한다. */
@Configuration(proxyBeanMethods = false)
public class AgreementTimeConfig {

  @Bean
  public Clock agreementClock() {
    return Clock.systemDefaultZone();
  }
}
