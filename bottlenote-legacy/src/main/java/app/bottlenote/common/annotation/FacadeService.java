package app.bottlenote.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.stereotype.Service;

/** 퍼사드 서비스 레이어를 표시하는 어노테이션. 이 어노테이션은 @Service를 포함하여 스프링 컴포넌트로 등록됩니다. */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Service
public @interface FacadeService {
  String value() default "";
}
