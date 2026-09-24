package app.external.systemone.jev.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** TypeSafe Jev 공급자 설정. API Key 외 값은 공식 기본값을 쓴다. */
@Getter
@Setter
@ConfigurationProperties(prefix = "systemone.jev")
public class JevProperties {
  private String baseUrl = "https://api.typesafe.ai";
  private String apiKey;
  private String model = "jev-latest";
  private Duration connectTimeout = Duration.ofSeconds(3);
  private Duration readTimeout = Duration.ofSeconds(10);

  public boolean hasApiKey() {
    return apiKey != null && !apiKey.isBlank();
  }
}
