package app.bottlenote.global.pagination;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@Tag("unit")
@DisplayName("PaginationConfiguration 단위 테스트")
class PaginationConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner().withUserConfiguration(PaginationConfiguration.class);

  @Test
  @DisplayName("Admin처럼 app.type이 product가 아니면 secret 없이 코덱 빈을 만든다")
  void missing_secret_allows_non_product_context() {
    contextRunner.run(
        context -> {
          assertThat(context).hasNotFailed();
          assertThat(context).hasSingleBean(HmacCursorCodec.class);
        });
  }

  @Test
  @DisplayName("Product는 current-secret이 없으면 애플리케이션 컨텍스트 로드가 실패한다")
  void missing_secret_fails_product_context_load() {
    contextRunner
        .withUserConfiguration(CursorSecretValidator.class)
        .withPropertyValues("app.type=product")
        .run(context -> assertThat(context).hasFailed());
  }

  @Test
  @DisplayName("current-secret이 있으면 HmacCursorCodec 빈을 만든다")
  void present_secret_creates_codec() {
    contextRunner
        .withPropertyValues(
            "bottlenote.pagination.cursor.current-key-id=v1",
            "bottlenote.pagination.cursor.current-secret=test-pagination-cursor-secret")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context).hasSingleBean(HmacCursorCodec.class);
              assertThat(context).hasSingleBean(CursorProperties.class);
            });
  }
}
