package app.bottlenote.observability.annotation;

import java.lang.annotation.*;

/** 메서드 실행을 추적하기 위한 어노테이션 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TraceMethod {

  /** 스팬(Span) 이름을 지정합니다. 지정하지 않으면 클래스명.메서드명으로 자동 생성됩니다. */
  String value() default "";

  /** 스팬에 추가할 태그들을 지정합니다. */
  String[] tags() default {};
}