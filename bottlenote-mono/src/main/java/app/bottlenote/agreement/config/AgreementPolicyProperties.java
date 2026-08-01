package app.bottlenote.agreement.config;

import app.bottlenote.agreement.constant.AgreementType;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "agreement.policy")
public class AgreementPolicyProperties {

  private Policy termsOfService = new Policy();
  private Policy privacyCollectionUse = new Policy();

  public Policy getPolicy(AgreementType type) {
    return switch (type) {
      case TERMS_OF_SERVICE -> termsOfService;
      case PRIVACY_COLLECTION_USE -> privacyCollectionUse;
    };
  }

  @Getter
  @Setter
  public static class Policy {

    private boolean required = true;
    private LocalDateTime effectiveFrom = LocalDateTime.of(2026, 8, 1, 0, 0);
  }
}
