package app.bottlenote.observability.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 컨트롤러 요청 로그(LoggerAspect)에서 성공 응답 로깅을 생략한다. 예외/비 2xx 응답은 여전히 기록된다.
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DisableLogger {}
