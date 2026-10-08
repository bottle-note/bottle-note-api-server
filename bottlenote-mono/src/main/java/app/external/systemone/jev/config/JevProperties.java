package app.external.systemone.jev.config;

import app.external.systemone.http.SystemOneProviderProperties;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** TypeSafe Jev 공급자 설정. API Key 외 값은 공식 기본값을 쓴다. */
@ConfigurationProperties(prefix = "systemone.jev")
public class JevProperties extends SystemOneProviderProperties {

  public JevProperties() {
    super("https://api.typesafe.ai", "jev-latest", Duration.ofSeconds(3), Duration.ofSeconds(10));
  }
}
