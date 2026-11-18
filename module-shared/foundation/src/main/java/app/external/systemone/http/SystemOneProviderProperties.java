package app.external.systemone.http;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;

/** HTTP 공급자 공통 설정. 공급자별 기본값은 하위 클래스가 생성자에서 정한다. */
@Getter
@Setter
public abstract class SystemOneProviderProperties {
  private String baseUrl;
  private String apiKey;
  private String model;
  private Duration connectTimeout;
  private Duration readTimeout;

  protected SystemOneProviderProperties(
      String baseUrl, String model, Duration connectTimeout, Duration readTimeout) {
    this.baseUrl = baseUrl;
    this.model = model;
    this.connectTimeout = connectTimeout;
    this.readTimeout = readTimeout;
  }

  public boolean hasApiKey() {
    return apiKey != null && !apiKey.isBlank();
  }
}
