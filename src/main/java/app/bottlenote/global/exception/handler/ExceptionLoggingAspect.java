package app.bottlenote.global.exception.handler;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 예외 로깅 관점 클래스입니다.
 * 이 클래스는 예외 핸들러 어노테이션이 붙은 메소드에서 예외가 발생할 경우 로깅합니다.
 */
@Slf4j
@Aspect
@Component
public class ExceptionLoggingAspect {

	/**
	 * 예외 핸들러 어노테이션이 붙은 메소드를 감싸서 예외를 로깅하는 메소드입니다.
	 *
	 * @param joinPoint 감싸고 있는 조인 포인트
	 * @return 조인 포인트의 결과
	 * @throws Throwable 예외가 발생하면 다시 던집니다.
	 */
	@Around("@annotation(org.springframework.web.bind.annotation.ExceptionHandler)")
	public Object logException(ProceedingJoinPoint joinPoint) throws Throwable {
		Object result;
		try {
			result = joinPoint.proceed();
		} catch (Exception exception) {
			log.error("예외 발생 : ", exception);
			throw exception;
		}
		return result;
	}
}
