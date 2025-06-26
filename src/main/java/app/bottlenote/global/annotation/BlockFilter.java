package app.bottlenote.global.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 차단된 사용자의 컨텐츠를 "차단된 사용자의 글입니다"로 처리하는 어노테이션
 * <p>
 * 사용 예시:
 * @BlockFilter(userField = "authorId")
 * @GetMapping("/reviews") public ResponseEntity<?> getReviews() { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BlockFilter {

	/**
	 * 응답 객체에서 사용자 ID를 추출할 필드명
	 * 예: "authorId", "userId", "writerId"
	 *
	 * @return 사용자 ID 필드명
	 */
	String userField() default "authorId";
}
