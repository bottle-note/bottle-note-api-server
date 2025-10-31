package app.bottlenote.observability.service;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.util.Map;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    value = "management.tracing.disabled",
    havingValue = "false",
    matchIfMissing = false)
public class MicrometerTracingService implements TracingService {

  private final Tracer tracer;

  @Override
  public <T> T withSpan(Logger logger, String spanName, Supplier<T> supplier) {
    return withSpan(logger, spanName, null, supplier);
  }

  @Override
  public <T> T withSpan(
      Logger logger, String spanName, Map<String, String> tags, Supplier<T> supplier) {
    Span span = tracer.nextSpan().name(spanName);

    try (Tracer.SpanInScope ws = tracer.withSpan(span.start())) {
      // 태그 추가
      if (tags != null) {
        tags.forEach(span::tag);
      }

      T result = supplier.get();
      span.tag("success", "true");

      return result;

    } catch (Exception e) {
      span.tag("success", "false");
      span.tag("error", e.getClass().getSimpleName());
      span.tag("error.message", e.getMessage());

      logger.error("Error in span: {}", spanName, e);
      throw e;

    } finally {
      span.end();
    }
  }

  @Override
  public void addTag(String key, String value) {
    Span currentSpan = tracer.currentSpan();
    if (currentSpan != null) {
      currentSpan.tag(key, value);
    }
  }

  @Override
  public void addTags(Map<String, String> tags) {
    Span currentSpan = tracer.currentSpan();
    if (currentSpan != null && tags != null) {
      tags.forEach(currentSpan::tag);
    }
  }

  @Override
  public void addEvent(String eventName) {
    Span currentSpan = tracer.currentSpan();
    if (currentSpan != null) {
      currentSpan.event(eventName);
    }
  }

  @Override
  public void addEvent(String eventName, Map<String, String> tags) {
    Span currentSpan = tracer.currentSpan();
    if (currentSpan != null) {
      if (tags != null && !tags.isEmpty()) {
        // 이벤트 태그들을 스팬 태그로 추가 (Micrometer의 제한)
        tags.forEach((key, value) -> currentSpan.tag("event." + key, value));
      }
      currentSpan.event(eventName);
    }
  }

  @Override
  public String getCurrentTraceId() {
    Span currentSpan = tracer.currentSpan();
    if (currentSpan != null && currentSpan.context() != null) {
      return currentSpan.context().traceId();
    }
    return null;
  }

  @Override
  public String getCurrentSpanId() {
    Span currentSpan = tracer.currentSpan();
    if (currentSpan != null && currentSpan.context() != null) {
      return currentSpan.context().spanId();
    }
    return null;
  }

  @Override
  public void setUserId(String userId) {
    if (userId != null) {
      try {
        tracer.createBaggageInScope("user.id", userId);
      } catch (Exception e) {
        log.debug("Failed to set baggage user.id: {}", e.getMessage());
      }
    }
  }

  @Override
  public void setTenantId(String tenantId) {
    if (tenantId != null) {
      try {
        tracer.createBaggageInScope("tenant.id", tenantId);
      } catch (Exception e) {
        log.debug("Failed to set baggage tenant.id: {}", e.getMessage());
      }
    }
  }

  @Override
  public String getUserId() {
    try {
      return tracer.getBaggage("user.id").get();
    } catch (Exception e) {
      log.debug("Failed to get baggage user.id: {}", e.getMessage());
      return null;
    }
  }

  @Override
  public String getTenantId() {
    try {
      return tracer.getBaggage("tenant.id").get();
    } catch (Exception e) {
      log.debug("Failed to get baggage tenant.id: {}", e.getMessage());
      return null;
    }
  }

  @Override
  public void recordException(Logger logger, Throwable exception) {
    Span currentSpan = tracer.currentSpan();
    if (currentSpan != null) {
      currentSpan.tag("error", "true");
      currentSpan.tag("exception.type", exception.getClass().getName());
      currentSpan.tag("exception.message", exception.getMessage());
    }
    logger.error("Exception recorded in span", exception);
  }

  @Override
  public void addAttribute(String key, String value) {
    addTag(key, value);
  }

  @Override
  public void addAttribute(String key, long value) {
    addTag(key, String.valueOf(value));
  }

  @Override
  public void addAttribute(String key, boolean value) {
    addTag(key, String.valueOf(value));
  }

  @Override
  public void addAttributes(Map<String, String> attributes) {
    addTags(attributes);
  }
}