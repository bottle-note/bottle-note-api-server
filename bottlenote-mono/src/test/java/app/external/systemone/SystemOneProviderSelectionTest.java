package app.external.systemone;

import static org.assertj.core.api.Assertions.assertThat;

import app.external.systemone.jev.DefaultJevSystemOneClient;
import app.external.systemone.jev.config.JevClientConfig;
import app.external.systemone.openai.DefaultOpenAiSystemOneClient;
import app.external.systemone.openai.config.OpenAiDecisionsClientConfig;
import app.external.systemone.openai.config.OpenAiDecisionsProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

@Tag("unit")
@DisplayName("System One 공급자 선택 단위 테스트")
class SystemOneProviderSelectionTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withBean(RestClient.Builder.class, RestClient::builder)
          .withBean(ObjectMapper.class, ObjectMapper::new)
          .withUserConfiguration(JevClientConfig.class, OpenAiDecisionsClientConfig.class);

  @Test
  @DisplayName("provider를 지정하지 않으면 Jev 클라이언트 하나만 등록한다")
  void provider_기본값은_Jev다() {
    contextRunner.run(
        context -> {
          assertThat(context).hasSingleBean(SystemOneClient.class);
          assertThat(context.getBean(SystemOneClient.class))
              .isInstanceOf(DefaultJevSystemOneClient.class);
        });
  }

  @Test
  @DisplayName("provider가 openai면 OpenAI 클라이언트 하나만 등록하고 기본 설정을 적용한다")
  void provider를_openai로_바꿀_수_있다() {
    contextRunner
        .withPropertyValues("systemone.provider=openai", "systemone.openai.api-key=test-key")
        .run(
            context -> {
              assertThat(context).hasSingleBean(SystemOneClient.class);
              assertThat(context.getBean(SystemOneClient.class))
                  .isInstanceOf(DefaultOpenAiSystemOneClient.class);
              OpenAiDecisionsProperties properties =
                  context.getBean(OpenAiDecisionsProperties.class);
              assertThat(properties.hasApiKey()).isTrue();
              assertThat(properties.getBaseUrl()).isEqualTo("https://api.openai.com");
              assertThat(properties.getModel()).isEqualTo("gpt-6-luna");
            });
  }
}
