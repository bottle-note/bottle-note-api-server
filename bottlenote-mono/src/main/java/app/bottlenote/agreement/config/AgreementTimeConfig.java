package app.bottlenote.agreement.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AgreementTimeConfig {

  @Bean
  public Clock agreementClock() {
    return Clock.systemDefaultZone();
  }
}
