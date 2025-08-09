package app.bottlenote.user.config;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Setter
@Getter
@Builder
@Configuration
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ConfigurationPropertiesScan
@ConfigurationProperties(prefix = "spring.oauth")
public class OauthConfigProperties {
  private String refreshTokenHeaderPrefix;
  private int cookieExpireTime;
  private String guestCode;

  public void printConfigs() {
    log.info("refreshTokenHeaderPrefix: {}", refreshTokenHeaderPrefix);
    log.info("cookieExpireTime: {}", cookieExpireTime);
    log.info("guestCode: {}", guestCode);
  }
}
