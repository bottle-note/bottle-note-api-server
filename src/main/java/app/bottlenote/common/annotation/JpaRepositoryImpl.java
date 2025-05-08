package app.bottlenote.common.annotation;


import org.springframework.stereotype.Repository;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JPA 구현체 리포지토리임을 나타내는 어노테이션
 *
 * <p>이 어노테이션은 다음과 같은 목적으로 사용됩니다:</p>
 * <ul>
 *   <li>JPA 기술을 사용하는 리포지토리 구현체 식별</li>
 *   <li>도메인 리포지토리 인터페이스의 JPA 구현임을 명시</li>
 *   <li>인프라스트럭처 레이어에 속하는 컴포넌트 구분</li>
 * </ul>
 *
 * <p>Spring의 @Repository를 포함하고 있어 자동으로 컴포넌트 스캔 및
 * 영속성 예외 변환 기능을 제공합니다.</p>
 *
 * <p>사용 예:</p>
 * <pre>
 * {@code
 * @JpaRepositoryImpl
 * public interface JpaUserRepository extends UserRepository, JpaRepository<User, UserId> {
 *     // 추가 JPA 특화 메소드
 * }
 * }
 * </pre>
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repository
public @interface JpaRepositoryImpl {
	String value() default "";
}
