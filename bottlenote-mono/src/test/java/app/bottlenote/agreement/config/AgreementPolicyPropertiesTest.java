package app.bottlenote.agreement.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@Tag("unit")
@DisplayName("AgreementPolicyProperties 바인딩 테스트")
class AgreementPolicyPropertiesTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
          .withUserConfiguration(AgreementPolicyProperties.class);

  @Test
  @DisplayName("설정이 없으면 두 유형에 필수 기본 정책을 적용한다")
  void bind_whenPropertiesAreMissing_usesRequiredDefaults() {
    contextRunner.run(
        context -> {
          AgreementPolicyProperties properties = context.getBean(AgreementPolicyProperties.class);

          assertThat(properties.getTermsOfService().isRequired()).isTrue();
          assertThat(properties.getPrivacyCollectionUse().isRequired()).isTrue();
          assertThat(properties.getTermsOfService().getEffectiveFrom())
              .isEqualTo(LocalDateTime.of(2026, 8, 1, 0, 0));
          assertThat(properties.getPrivacyCollectionUse().getEffectiveFrom())
              .isEqualTo(LocalDateTime.of(2026, 8, 1, 0, 0));
        });
  }

  @Test
  @DisplayName("유형별 필수 여부와 기준 시각을 외부 설정으로 변경한다")
  void bind_whenPropertiesAreProvided_overridesEachPolicy() {
    contextRunner
        .withPropertyValues(
            "agreement.policy.terms-of-service.required=false",
            "agreement.policy.terms-of-service.effective-from=2026-09-01T12:30:00",
            "agreement.policy.privacy-collection-use.required=true",
            "agreement.policy.privacy-collection-use.effective-from=2026-10-02T03:04:05")
        .run(
            context -> {
              AgreementPolicyProperties properties =
                  context.getBean(AgreementPolicyProperties.class);

              assertThat(properties.getTermsOfService().isRequired()).isFalse();
              assertThat(properties.getTermsOfService().getEffectiveFrom())
                  .isEqualTo(LocalDateTime.of(2026, 9, 1, 12, 30));
              assertThat(properties.getPrivacyCollectionUse().isRequired()).isTrue();
              assertThat(properties.getPrivacyCollectionUse().getEffectiveFrom())
                  .isEqualTo(LocalDateTime.of(2026, 10, 2, 3, 4, 5));
            });
  }
}
