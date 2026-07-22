package app.bottlenote.observability.visitor;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** VisitorTelemetry Redis Stream 설정이다. */
@ConfigurationProperties(prefix = "bottlenote.observability.visitor-telemetry")
public class VisitorTelemetryProperties {

  private boolean enabled;
  private String streamKey = "visitor-telemetry";
  private long maxLength = 100_000L;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getStreamKey() {
    return streamKey;
  }

  public void setStreamKey(String streamKey) {
    this.streamKey = streamKey;
  }

  public long getMaxLength() {
    return maxLength;
  }

  public void setMaxLength(long maxLength) {
    this.maxLength = maxLength;
  }
}
