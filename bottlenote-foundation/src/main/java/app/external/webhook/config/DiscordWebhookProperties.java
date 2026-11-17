package app.external.webhook.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "webhook.discord")
public class DiscordWebhookProperties {
  private String url;
}
