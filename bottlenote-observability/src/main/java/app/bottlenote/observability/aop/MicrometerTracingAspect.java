package app.bottlenote.observability.aop;

import app.bottlenote.observability.annotation.TraceMethod;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    value = "management.tracing.disabled",
    havingValue = "false",
    matchIfMissing = false)
public class MicrometerTracingAspect {

  private final Tracer tracer;

  @Around("@annotation(traceMethod)")
  public Object traceMethod(ProceedingJoinPoint joinPoint, TraceMethod traceMethod)
      throws Throwable {
    MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
    String className = methodSignature.getDeclaringType().getSimpleName();
    String methodName = methodSignature.getName();

    // 스팬 이름 결정
    String spanName =
        traceMethod.value().isEmpty() ? className + "." + methodName : traceMethod.value();

    Span span = tracer.nextSpan().name(spanName);

    try (Tracer.SpanInScope ws = tracer.withSpan(span.start())) {
      // 기본 태그 추가
      span.tag("class", className);
      span.tag("method", methodName);
      span.tag("component", "application");

      // 커스텀 태그 추가
      String[] tags = traceMethod.tags();
      for (int i = 0; i < tags.length; i += 2) {
        if (i + 1 < tags.length) {
          span.tag(tags[i], tags[i + 1]);
        }
      }

      // 메서드 실행
      Object result = joinPoint.proceed();

      // 성공 태그 추가
      span.tag("success", "true");

      return result;

    } catch (Exception e) {
      // 실패 태그 및 에러 정보 추가
      span.tag("success", "false");
      span.tag("error", e.getClass().getSimpleName());
      span.tag("error.message", e.getMessage());
      span.error(e);

      log.error("Error in traced method: {}.{}", className, methodName, e);

      throw e;
    } finally {
      span.end();
    }
  }

  /** Service 클래스의 모든 public 메서드를 자동으로 추적 */
  @Around("execution(public * app.bottlenote..service.*.*(..))")
  public Object traceServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
    MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
    String className = methodSignature.getDeclaringType().getSimpleName();
    String methodName = methodSignature.getName();

    String spanName = "service." + className + "." + methodName;
    Span span = tracer.nextSpan().name(spanName);

    try (Tracer.SpanInScope ws = tracer.withSpan(span.start())) {
      span.tag("layer", "service");
      span.tag("class", className);
      span.tag("method", methodName);
      span.tag("component", "service");

      Object result = joinPoint.proceed();
      span.tag("success", "true");

      return result;

    } catch (Exception e) {
      span.tag("success", "false");
      span.tag("error", e.getClass().getSimpleName());
      span.tag("error.message", e.getMessage());
      span.error(e);

      throw e;
    } finally {
      span.end();
    }
  }

  /** Repository 클래스의 모든 public 메서드를 자동으로 추적 */
  @Around("execution(public * app.bottlenote..repository.*.*(..))")
  public Object traceRepositoryMethods(ProceedingJoinPoint joinPoint) throws Throwable {
    MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
    String className = methodSignature.getDeclaringType().getSimpleName();
    String methodName = methodSignature.getName();

    String spanName = "repository." + className + "." + methodName;
    Span span = tracer.nextSpan().name(spanName);

    try (Tracer.SpanInScope ws = tracer.withSpan(span.start())) {
      span.tag("layer", "repository");
      span.tag("class", className);
      span.tag("method", methodName);
      span.tag("component", "repository");
      span.tag("db.operation.type", "repository_call");

      Object result = joinPoint.proceed();
      span.tag("success", "true");

      return result;

    } catch (Exception e) {
      span.tag("success", "false");
      span.tag("error", e.getClass().getSimpleName());
      span.tag("error.message", e.getMessage());
      span.error(e);

      throw e;
    } finally {
      span.end();
    }
  }

  /** Controller 클래스의 모든 public 메서드를 자동으로 추적 */
  @Around("execution(public * app.bottlenote..controller.*.*(..))")
  public Object traceControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
    MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
    String className = methodSignature.getDeclaringType().getSimpleName();
    String methodName = methodSignature.getName();

    if (className.equals("HealthCheckController")) {
      // HealthCheckController는 추적하지 않음
      return joinPoint.proceed();
    }

    String spanName = "controller." + className + "." + methodName;
    Span span = tracer.nextSpan().name(spanName);

    try (Tracer.SpanInScope ws = tracer.withSpan(span.start())) {
      span.tag("layer", "controller");
      span.tag("class", className);
      span.tag("method", methodName);
      span.tag("component", "controller");
      span.tag("http.handler", "spring-mvc");

      Object result = joinPoint.proceed();
      span.tag("success", "true");

      return result;

    } catch (Exception e) {
      span.tag("success", "false");
      span.tag("error", e.getClass().getSimpleName());
      span.tag("error.message", e.getMessage());
      span.error(e);

      throw e;
    } finally {
      span.end();
    }
  }
}
