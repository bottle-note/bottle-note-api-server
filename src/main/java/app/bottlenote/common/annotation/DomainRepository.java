package app.bottlenote.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * 도메인 계층의 순수한 리포지토리 인터페이스를 표시하는 어노테이션
 *
 * <p>이 어노테이션은 다음과 같은 목적으로 사용됩니다:</p>
 * <ul>
 *   <li>인프라스트럭처(예: JPA, MongoDB 등)에 의존하지 않는 순수 도메인 리포지토리 식별</li>
 *   <li>도메인 레이어와 인프라 레이어 간의 명확한 경계 설정</li>
 *   <li>도메인 주도 설계(DDD)의 원칙에 따라 기술적 관심사와 비즈니스 관심사 분리</li>
 * </ul>
 *
 * <p>Spring의 @Repository와 달리 이 어노테이션은 프레임워크에 종속되지 않으며,
 * 영속성 예외 변환과 같은 기능을 제공하지 않습니다. 순수하게 도메인 개념임을 나타내는 마커입니다.</p>
 *
 * <p>사용 예:</p>
 * <pre>
 * {@code
 * @DomainRepository
 * public interface UserRepository {
 *     User save(User user);
 *     Optional<User> findById(UserId id);
 *     // ...
 * }
 * }
 * </pre>
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DomainRepository {
}
