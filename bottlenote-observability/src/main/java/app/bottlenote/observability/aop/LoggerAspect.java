package app.bottlenote.observability.aop;

import app.bottlenote.observability.annotation.DisableLogger;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
@Aspect
@Slf4j
public class LoggerAspect {

  private static final long SLOW_REQUEST_THRESHOLD_MS = 1000L;

  private final RequestMappingHandlerMapping handlerMapping;
  private Set<String> loggerIgnorePatterns;

  public LoggerAspect(
      @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping) {
    this.handlerMapping = handlerMapping;
  }

  @PostConstruct
  public void initialize() {
    loggerIgnorePatterns = findDisableLoggerPatterns();
  }

  @Around("execution(* app.bottlenote..controller.*.*(..))")
  public Object methodLogger(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
    var start = System.currentTimeMillis();
    Object result = null;
    Throwable thrown = null;

    RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
    HttpServletRequest request = null;
    HttpServletResponse response = null;
    if (attributes instanceof ServletRequestAttributes servletAttrs) {
      request = servletAttrs.getRequest();
      response = servletAttrs.getResponse();
    }

    String controllerName = proceedingJoinPoint.getSignature().getDeclaringType().getSimpleName();
    String methodName = proceedingJoinPoint.getSignature().getName();
    Map<String, Object> params = new HashMap<>();

    try {
      result = proceedingJoinPoint.proceed(proceedingJoinPoint.getArgs());
      return result;
    } catch (Throwable t) {
      thrown = t;
      throw t;
    } finally {
      long time = System.currentTimeMillis() - start;
      boolean isIgnore = false;
      boolean isResponseError = false;
      Integer statusCode = null;

      if (request != null && loggerIgnorePatterns != null) {
        isIgnore = loggerIgnorePatterns.contains(request.getRequestURI());
      }
      if (result instanceof ResponseEntity<?> responseEntity) {
        statusCode = responseEntity.getStatusCode().value();
        isResponseError = !responseEntity.getStatusCode().is2xxSuccessful();
      } else if (response != null) {
        statusCode = response.getStatus();
      }

      if (thrown != null || isResponseError || !isIgnore) {
        try {
          params.put("controller", controllerName);
          params.put("method", methodName);
          params.put("log_time", new Date());
          if (request != null) {
            params.put("request_uri", request.getRequestURI());
            params.put("http_method", request.getMethod());
          }
          String traceId = MDC.get("traceId");
          if (traceId != null) {
            params.put("trace_id", traceId);
          }
          if (thrown != null) {
            params.put("exception", thrown.getClass().getSimpleName());
          }
          if (statusCode != null) {
            params.put("response_status", statusCode);
          }
        } catch (Exception e) {
          log.error("LoggerAspect error", e);
        }

        boolean slow = time >= SLOW_REQUEST_THRESHOLD_MS;
        if (thrown != null || isResponseError || slow) {
          log.warn("time : {}ms, params : {}", time, params);
        } else {
          log.info("time : {}ms, params : {}", time, params);
        }
      }
    }
  }

  private Set<String> findDisableLoggerPatterns() {
    Set<String> patterns = new HashSet<>();
    Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();
    for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
      Method method = entry.getValue().getMethod();
      Class<?> declaringClass = method.getDeclaringClass();
      boolean disabled =
          AnnotatedElementUtils.hasAnnotation(method, DisableLogger.class)
              || AnnotatedElementUtils.hasAnnotation(declaringClass, DisableLogger.class);
      if (disabled) {
        patterns.addAll(entry.getKey().getPatternValues());
      }
    }
    return patterns;
  }
}
