package app.bottlenote.global.pagination;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bottlenote.pagination.cursor")
public class CursorProperties implements InitializingBean {

  private String currentKeyId = "v1";
  private String currentSecret;
  private String previousKeyId;
  private String previousSecret;

  @Override
  public void afterPropertiesSet() {
    validate();
  }

  public void validate() {
    if (isBlank(currentKeyId) || isBlank(currentSecret)) {
      throw new IllegalStateException(
          "bottlenote.pagination.cursor.current-secret and current-key-id must be set");
    }
    if (!isBlank(previousKeyId) && isBlank(previousSecret)) {
      throw new IllegalStateException(
          "bottlenote.pagination.cursor.previous-secret is required when previous-key-id is set");
    }
    if (!isBlank(previousKeyId) && previousKeyId.equals(currentKeyId)) {
      throw new IllegalStateException("previous-key-id must differ from current-key-id");
    }
  }

  public String getCurrentKeyId() {
    return currentKeyId;
  }

  public void setCurrentKeyId(String currentKeyId) {
    this.currentKeyId = currentKeyId;
  }

  public String getCurrentSecret() {
    return currentSecret;
  }

  public void setCurrentSecret(String currentSecret) {
    this.currentSecret = currentSecret;
  }

  public String getPreviousKeyId() {
    return previousKeyId;
  }

  public void setPreviousKeyId(String previousKeyId) {
    this.previousKeyId = previousKeyId;
  }

  public String getPreviousSecret() {
    return previousSecret;
  }

  public void setPreviousSecret(String previousSecret) {
    this.previousSecret = previousSecret;
  }

  public byte[] currentSecretBytes() {
    return utf8(currentSecret);
  }

  public Optional<byte[]> secretFor(String keyId) {
    if (keyId == null) {
      return Optional.empty();
    }
    if (keyId.equals(currentKeyId)) {
      return Optional.of(utf8(currentSecret));
    }
    if (!isBlank(previousKeyId) && keyId.equals(previousKeyId)) {
      return Optional.of(utf8(previousSecret));
    }
    return Optional.empty();
  }

  private static byte[] utf8(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
