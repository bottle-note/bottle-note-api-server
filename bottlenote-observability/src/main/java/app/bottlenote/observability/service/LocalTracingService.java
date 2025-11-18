package app.bottlenote.observability.service;

import java.util.Map;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(
    value = "management.tracing.disabled",
    havingValue = "true",
    matchIfMissing = true)
public class LocalTracingService implements TracingService {

  private static final String LOCAL_TRACE_ID = "N/A";

  @Override
  public <T> T withSpan(Logger logger, String spanName, Supplier<T> supplier) {
    return supplier.get();
  }

  @Override
  public <T> T withSpan(
      Logger logger, String spanName, Map<String, String> tags, Supplier<T> supplier) {
    return supplier.get();
  }

  @Override
  public void addTag(String key, String value) {}

  @Override
  public void addTags(Map<String, String> tags) {}

  @Override
  public void addEvent(String eventName) {}

  @Override
  public void addEvent(String eventName, Map<String, String> tags) {}

  @Override
  public String getCurrentTraceId() {
    return LOCAL_TRACE_ID;
  }

  @Override
  public String getCurrentSpanId() {
    return LOCAL_TRACE_ID;
  }

  @Override
  public void setUserId(String userId) {}

  @Override
  public void setTenantId(String tenantId) {}

  @Override
  public String getUserId() {
    return null;
  }

  @Override
  public String getTenantId() {
    return null;
  }

  @Override
  public void recordException(Logger logger, Throwable exception) {
    logger.error("Exception recorded", exception);
  }

  @Override
  public void addAttribute(String key, String value) {}

  @Override
  public void addAttribute(String key, long value) {}

  @Override
  public void addAttribute(String key, boolean value) {}

  @Override
  public void addAttributes(Map<String, String> attributes) {}
}
