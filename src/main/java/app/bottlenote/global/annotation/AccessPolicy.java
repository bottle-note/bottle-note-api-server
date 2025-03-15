package app.bottlenote.global.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AccessPolicy {
	/**
	 * 접근 제어 유형
	 */
	AccessType type() default AccessType.ALL;

	/**
	 * PathVariable에서 사용자 ID를 가져올 변수명
	 */
	String key() default "userId";

	enum AccessType {
		ALL, // 모든 유저
		OWNER, // 본인만
		ADMIN // 관리자만
	}
}
