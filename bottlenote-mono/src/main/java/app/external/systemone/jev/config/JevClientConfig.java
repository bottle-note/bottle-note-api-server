package app.external.systemone.jev.config;

import app.external.systemone.SystemOneClient;
import app.external.systemone.jev.DefaultJevSystemOneClient;
import app.external.systemone.jev.JevHttpTransport;
import app.external.systemone.jev.v1.JevV1Mapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(JevProperties.class)
public class JevClientConfig {

  @Bean
  public SystemOneClient jevSystemOneClient(
      RestClient.Builder restClientBuilder, JevProperties properties, ObjectMapper objectMapper) {
    JevHttpTransport transport = JevHttpTransport.create(restClientBuilder, properties);
    return new DefaultJevSystemOneClient(
        transport, new JevV1Mapper(objectMapper), properties, objectMapper);
  }
}
