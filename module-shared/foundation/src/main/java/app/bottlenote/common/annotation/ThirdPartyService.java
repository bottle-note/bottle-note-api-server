package app.bottlenote.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.stereotype.Service;

/**
 * 외부 써드파티 시스템과 통신하는 서비스 레이어를 표시하는 어노테이션. 이 어노테이션이 적용된 서비스는 데이터베이스 트랜잭션이 필요하지 않습니다. AWS, 외부 API 호출 등
 * 외부 시스템과의 통합을 담당하는 서비스에 사용됩니다. 이 어노테이션은 @Service를 포함하여 스프링 컴포넌트로 등록됩니다.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Service
public @interface ThirdPartyService {
  String value() default "";
}
