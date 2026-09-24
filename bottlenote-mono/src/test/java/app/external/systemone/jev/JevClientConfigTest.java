package app.external.systemone.jev;

import static org.assertj.core.api.Assertions.assertThat;

import app.external.systemone.SystemOneClient;
import app.external.systemone.dto.request.SystemOneQuestion;
import app.external.systemone.dto.request.SystemOneRequest;
import app.external.systemone.dto.response.SystemOneFailureType;
import app.external.systemone.dto.response.SystemOneResult;
import app.external.systemone.jev.config.JevClientConfig;
import app.external.systemone.jev.config.JevProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

@Tag("unit")
@DisplayName("JevClientConfig 단위 테스트")
class JevClientConfigTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withBean(RestClient.Builder.class, RestClient::builder)
          .withBean(ObjectMapper.class, ObjectMapper::new)
          .withUserConfiguration(JevClientConfig.class);

  @Test
  @DisplayName("API Key가 없어도 컨텍스트가 기동하고 호출하면 NOT_CONFIGURED를 반환한다")
  void API_Key_없이_기동할_수_있다() {
    contextRunner.run(
        context -> {
          assertThat(context).hasNotFailed();
          SystemOneResult result =
              context
                  .getBean(SystemOneClient.class)
                  .evaluate(SystemOneRequest.of("state", "k", new SystemOneQuestion.Noul("q")));
          assertThat(result)
              .isInstanceOfSatisfying(
                  SystemOneResult.Failure.class,
                  failure -> assertThat(failure.type()).isEqualTo(SystemOneFailureType.NOT_CONFIGURED));
        });
  }

  @Test
  @DisplayName("API Key만 주입하면 나머지 설정은 공식 기본값을 사용한다")
  void API_Key만_주입할_수_있다() {
    contextRunner
        .withPropertyValues("systemone.jev.api-key=test-key")
        .run(
            context -> {
              JevProperties properties = context.getBean(JevProperties.class);
              assertThat(properties.hasApiKey()).isTrue();
              assertThat(properties.getBaseUrl()).isEqualTo("https://api.typesafe.ai");
              assertThat(properties.getModel()).isEqualTo("jev-latest");
            });
  }
}
