package app.external.systemone.jev.config;

import app.external.systemone.SystemOneClient;
import app.external.systemone.http.SystemOneHttpTransport;
import app.external.systemone.jev.DefaultJevSystemOneClient;
import app.external.systemone.jev.v1.JevV1Mapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** {@code systemone.provider}가 jev이거나 비어 있을 때 Jev를 System One 공급자로 등록한다. */
@Configuration
@ConditionalOnProperty(
    prefix = "systemone",
    name = "provider",
    havingValue = "jev",
    matchIfMissing = true)
@EnableConfigurationProperties(JevProperties.class)
public class JevClientConfig {

  @Bean
  public SystemOneClient jevSystemOneClient(
      RestClient.Builder restClientBuilder, JevProperties properties, ObjectMapper objectMapper) {
    SystemOneHttpTransport transport =
        SystemOneHttpTransport.create(
            restClientBuilder, properties, DefaultJevSystemOneClient.SYSTEM_ONE_PATH);
    return new DefaultJevSystemOneClient(
        transport, new JevV1Mapper(objectMapper), properties, objectMapper);
  }
}
