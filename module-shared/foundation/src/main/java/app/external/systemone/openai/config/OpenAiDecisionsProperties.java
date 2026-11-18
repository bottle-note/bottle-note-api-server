package app.external.systemone.openai.config;

import app.external.systemone.http.SystemOneProviderProperties;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** OpenAI Decisions API 공급자 설정. API Key 외 값은 공식 기본값을 쓴다. */
@ConfigurationProperties(prefix = "systemone.openai")
public class OpenAiDecisionsProperties extends SystemOneProviderProperties {

  public OpenAiDecisionsProperties() {
    super("https://api.openai.com", "gpt-6-luna", Duration.ofSeconds(3), Duration.ofSeconds(10));
  }
}
