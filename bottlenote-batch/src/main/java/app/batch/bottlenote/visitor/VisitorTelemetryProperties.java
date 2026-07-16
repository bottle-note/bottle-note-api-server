package app.batch.bottlenote.visitor;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "batch.visitor-telemetry")
class VisitorTelemetryProperties {

  private boolean enabled;
  private String streamKey = "visitor-telemetry";
  private String groupName = "visitor-telemetry-writers";
  private String deadLetterStreamKey = "visitor-telemetry-dead-letter";
  private String consumerName = System.getenv().getOrDefault("HOSTNAME", "bottlenote-batch-local");
  private int batchSize = 200;
  private Duration blockTimeout = Duration.ofSeconds(2);
  private Duration retryBackoff = Duration.ofSeconds(5);
  private Duration pendingIdle = Duration.ofMinutes(1);
  private long deadLetterMaxLength = 10_000L;

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

  public String getGroupName() {
    return groupName;
  }

  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }

  public String getDeadLetterStreamKey() {
    return deadLetterStreamKey;
  }

  public void setDeadLetterStreamKey(String deadLetterStreamKey) {
    this.deadLetterStreamKey = deadLetterStreamKey;
  }

  public String getConsumerName() {
    return consumerName;
  }

  public void setConsumerName(String consumerName) {
    this.consumerName = consumerName;
  }

  public int getBatchSize() {
    return batchSize;
  }

  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
  }

  public Duration getBlockTimeout() {
    return blockTimeout;
  }

  public void setBlockTimeout(Duration blockTimeout) {
    this.blockTimeout = blockTimeout;
  }

  public Duration getRetryBackoff() {
    return retryBackoff;
  }

  public void setRetryBackoff(Duration retryBackoff) {
    this.retryBackoff = retryBackoff;
  }

  public Duration getPendingIdle() {
    return pendingIdle;
  }

  public void setPendingIdle(Duration pendingIdle) {
    this.pendingIdle = pendingIdle;
  }

  public long getDeadLetterMaxLength() {
    return deadLetterMaxLength;
  }

  public void setDeadLetterMaxLength(long deadLetterMaxLength) {
    this.deadLetterMaxLength = deadLetterMaxLength;
  }
}
