package app.bottlenote.global.pagination;

import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CursorProperties.class)
public class PaginationConfiguration {

  @Bean
  public HmacCursorCodec hmacCursorCodec(
      CursorProperties properties, ObjectProvider<Clock> clockProvider) {
    return new HmacCursorCodec(properties, clockProvider.getIfAvailable(Clock::systemUTC));
  }
}
