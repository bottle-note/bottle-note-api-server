package app.external.systemone.openai.config;

import app.external.systemone.SystemOneClient;
import app.external.systemone.http.SystemOneHttpTransport;
import app.external.systemone.openai.DefaultOpenAiSystemOneClient;
import app.external.systemone.openai.OpenAiDecisionsMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** {@code systemone.provider=openai}일 때 OpenAI Decisions API를 System One 공급자로 등록한다. */
@Configuration
@ConditionalOnProperty(prefix = "systemone", name = "provider", havingValue = "openai")
@EnableConfigurationProperties(OpenAiDecisionsProperties.class)
public class OpenAiDecisionsClientConfig {

  @Bean
  public SystemOneClient openAiSystemOneClient(
      RestClient.Builder restClientBuilder,
      OpenAiDecisionsProperties properties,
      ObjectMapper objectMapper) {
    SystemOneHttpTransport transport =
        SystemOneHttpTransport.create(
            restClientBuilder, properties, DefaultOpenAiSystemOneClient.DECISIONS_PATH);
    return new DefaultOpenAiSystemOneClient(
        transport, new OpenAiDecisionsMapper(objectMapper), properties, objectMapper);
  }
}
