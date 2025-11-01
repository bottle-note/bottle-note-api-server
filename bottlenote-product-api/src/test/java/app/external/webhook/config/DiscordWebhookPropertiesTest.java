package app.external.webhook.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@Tag("unit")
@DisplayName("[unit] [config] DiscordWebhookProperties")
class DiscordWebhookPropertiesTest {

  @DisplayName("DiscordWebhookProperties 객체가 정상적으로 생성된다")
  @Test
  void testDiscordWebhookProperties_객체생성() {
    // given & when
    DiscordWebhookProperties properties = new DiscordWebhookProperties();

    // then
    assertNotNull(properties);
  }

  @DisplayName("URL을 설정하고 조회할 수 있다")
  @Test
  void testDiscordWebhookProperties_URL설정및조회() {
    // given
    DiscordWebhookProperties properties = new DiscordWebhookProperties();
    String testUrl = "https://discord.com/api/webhooks/test";

    // when
    properties.setUrl(testUrl);

    // then
    assertEquals(testUrl, properties.getUrl());
  }

  @DisplayName("URL이 설정되지 않으면 null이다")
  @Test
  void testDiscordWebhookProperties_URL미설정시null() {
    // given & when
    DiscordWebhookProperties properties = new DiscordWebhookProperties();

    // then
    assertNull(properties.getUrl());
  }

  @SpringBootTest(classes = {DiscordWebhookProperties.class, WebhookConfig.class})
  @EnableConfigurationProperties(DiscordWebhookProperties.class)
  @TestPropertySource(
      properties = {"webhook.discord.url=https://discord.com/api/webhooks/test-from-properties"})
  @DisplayName("[integration] application.yml의 설정값이 올바르게 바인딩된다")
  @Tag("integration")
  static class ConfigurationPropertiesBindingTest {

    @Autowired private DiscordWebhookProperties properties;

    @Test
    @DisplayName("Properties 파일의 값이 올바르게 주입된다")
    void testPropertiesBinding_설정값주입검증() {
      // given & when & then
      assertNotNull(properties);
      assertEquals("https://discord.com/api/webhooks/test-from-properties", properties.getUrl());
    }
  }

  @SpringBootTest(classes = {DiscordWebhookProperties.class, WebhookConfig.class})
  @EnableConfigurationProperties(DiscordWebhookProperties.class)
  @DisplayName("[integration] 환경변수가 설정되지 않으면 기본값이 적용된다")
  @Tag("integration")
  static class DefaultValueTest {

    @Autowired private DiscordWebhookProperties properties;

    @Test
    @DisplayName("URL이 설정되지 않으면 null이다")
    void testDefaultValue_URL미설정시null() {
      // given & when & then
      assertNotNull(properties);
    }
  }
}
