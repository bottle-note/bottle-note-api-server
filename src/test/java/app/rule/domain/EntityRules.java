package app.rule.domain;

import app.rule.AbstractRules;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import org.hibernate.annotations.Comment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;

/**
 * 도메인 엔티티 클래스의 아키텍처 규칙을 검증하는 테스트 클래스입니다.
 */
@Tag("rule")
@DisplayName("도메인 엔티티 아키텍처 규칙")
@SuppressWarnings({"NonAsciiCharacters", "JUnitTestClassNamingConvention"})
public class EntityRules extends AbstractRules {
	/**
	 * 모든 @Entity 어노테이션이 있는 클래스에 명시적 이름이 선언되어 있는지 검증합니다.
	 */
	@Test
	public void 엔티티_명시적_이름_검증() {
		ArchRule rule = classes()
			.that().areAnnotatedWith(Entity.class)
			.should(new ArchCondition<>("@Entity에 명시적인 이름이 선언되어야 함") {
				@Override
				public void check(JavaClass javaClass, ConditionEvents events) {
					Entity entityAnnotation = javaClass.getAnnotationOfType(Entity.class);
					String name = entityAnnotation.name();

					boolean hasExplicitName = name != null && !name.isEmpty();
					String message = String.format(
						"엔티티 클래스 '%s'는 @Entity 어노테이션에 명시적 이름이 %s",
						javaClass.getName(),
						hasExplicitName ? "선언되어 있습니다" : "선언되어 있지 않습니다"
					);

					if (hasExplicitName) {
						events.add(SimpleConditionEvent.satisfied(javaClass, message));
					} else {
						events.add(SimpleConditionEvent.violated(javaClass, message));
					}
				}
			})
			.because("모든 엔티티 클래스는 @Entity(name = \"명시적_이름\") 형태로 선언되어야 합니다");

		rule.check(importedClasses);
	}

	/**
	 * 모든 엔티티 클래스가 protected 접근 수준의 기본 생성자를 가지는지 검증합니다.
	 * Lombok의 @NoArgsConstructor는 SOURCE 리텐션이라 직접 확인할 수 없으므로 생성자를 확인합니다.
	 */
	@Test
	public void 엔티티_기본생성자_검증() {
		ArchRule rule = classes()
			.that().areAnnotatedWith(Entity.class)
			.should(new ArchCondition<>("protected 접근 수준의 기본 생성자를 가져야 함") {
				@Override
				public void check(JavaClass javaClass, ConditionEvents events) {
					boolean hasProtectedNoArgsConstructor = javaClass.getConstructors().stream()
						.anyMatch(constructor ->
							constructor.getParameters().isEmpty() &&
								constructor.getModifiers().contains(JavaModifier.PROTECTED));

					String message = String.format(
						"엔티티 클래스 '%s'는 %s",
						javaClass.getName(),
						hasProtectedNoArgsConstructor
							? "protected 접근 수준의 기본 생성자를 가지고 있습니다"
							: "protected 접근 수준의 기본 생성자가 없습니다"
					);

					if (hasProtectedNoArgsConstructor) {
						events.add(SimpleConditionEvent.satisfied(javaClass, message));
					} else {
						events.add(SimpleConditionEvent.violated(javaClass, message));
					}
				}
			})
			.because("엔티티 클래스는 protected 접근 수준의 기본 생성자를 가져야 합니다");

		rule.check(importedClasses);
	}

	/**
	 * 엔티티 클래스의 모든 필드가 @Comment 어노테이션을 가지는지 검증합니다.
	 */
	@Test
	public void 엔티티_필드_주석_검증() {
		ArchRule rule = fields()
			.that().areDeclaredInClassesThat().areAnnotatedWith(Entity.class)

			// 제외 대상 필드
			.and().areNotAnnotatedWith(Id.class)
			.and().areNotAnnotatedWith(ManyToOne.class)
			.and().areNotAnnotatedWith(OneToOne.class)
			.and().areNotAnnotatedWith(ManyToMany.class)
			.and().areNotAnnotatedWith(Embedded.class)
			.and().areNotAnnotatedWith(EmbeddedId.class)
			.and().areNotStatic()

			.should().beAnnotatedWith(Comment.class)
			.because("엔티티 클래스의 모든 필드는 @Comment 어노테이션으로 설명을 추가해야 합니다");

		rule.check(importedClasses);
	}

	/**
	 * 모든 엔티티 클래스가 올바른 패키지에 위치하는지 검증합니다.
	 */
	@Test
	public void 엔티티_패키지_위치_검증() {
		ArchRule rule = classes()
			.that().areAnnotatedWith(Entity.class)
			.should().resideInAPackage("..domain..")
			.because("모든 엔티티 클래스는 'app.bottlenote.*.domain' 패키지에 위치해야 합니다");

		rule.check(importedClasses);
	}
}
