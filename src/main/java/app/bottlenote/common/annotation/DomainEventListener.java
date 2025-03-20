package app.bottlenote.common.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface DomainEventListener {
	/**
	 * 이벤트 처리 방식을 지정합니다.
	 */
	ProcessingType type() default ProcessingType.ASYNCHRONOUS;

	/**
	 * 이벤트 처리 방식
	 */
	enum ProcessingType {
		/**
		 * 동기식 이벤트 처리
		 */
		SYNCHRONOUS,

		/**
		 * 비동기식 이벤트 처리
		 */
		ASYNCHRONOUS
	}
}
