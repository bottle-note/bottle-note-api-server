package app.bottlenote.observability.config;

import io.micrometer.observation.ObservationPredicate;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.observation.ServerRequestObservationContext;

@Slf4j
@Configuration
@ConditionalOnProperty(
    value = "management.tracing.disabled",
    havingValue = "false",
    matchIfMissing = false)
public class TracingConfiguration {

  @Value("${management.tracing.disabled}")
  private boolean tracingDisabled;

  @Value("${spring.application.name}")
  private String applicationName;

  @Value("${spring.application.version:unknown}")
  private String applicationVersion;

  @Value("${management.tracing.sampling.probability:1.0}")
  private float samplingProbability;

  @Value("${management.otlp.tracing.endpoint:unknown}")
  private String otlpTraceEndpoint;

  @Value("${management.otlp.tracing.protocol:unknown}")
  private String protocol;

  @PostConstruct
  void tracingInfo() {
    log.info("OpenTelemetry OTLP tracing configured successfully");
    log.info("Service: {}, Version: {}", applicationName, applicationVersion);
    log.info("OTLP Endpoint: {}", otlpTraceEndpoint);
    log.info("Sampling Probability: {}", samplingProbability);
    log.info("Protocol: {}", protocol);
    log.info("Using Spring Boot auto-configuration for OpenTelemetry");
  }

  @Bean
  @ConditionalOnProperty(value = "management.tracing.disabled", havingValue = "false")
  public ApplicationListener<ApplicationReadyEvent> openTelemetryLogbackAppenderInitializer(
      OpenTelemetry openTelemetry) {
    return event -> {
      OpenTelemetryAppender.install(openTelemetry);
      log.info("OpenTelemetryAppender installed");
    };
  }

  @Bean
  @ConditionalOnProperty(value = "management.tracing.disabled", havingValue = "false")
  public ObservationPredicate skipActuatorEndpoints() {
    return (name, context) -> {
      // Scheduled task 제외
      if (name.startsWith("task")) {
        log.debug("Skipping tracing for scheduled task: {}", name);
        return false;
      }

      // Actuator 엔드포인트 제외
      if (context instanceof ServerRequestObservationContext serverContext) {
        String uri = serverContext.getCarrier().getRequestURI();
        boolean shouldObserve = !uri.startsWith("/actuator");
        if (!shouldObserve) {
          log.debug("Skipping tracing for actuator endpoint: {}", uri);
        }
        return shouldObserve;
      }
      return true;
    };
  }
}
