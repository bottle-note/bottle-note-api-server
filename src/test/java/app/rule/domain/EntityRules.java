package app.rule.domain;

import app.rule.AbstractRules;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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

	/**
	 * 엔티티의 toString 메서드가 무한 순환 참조를 방지하는지 검증합니다.
	 */
	@Test
	public void 엔티티_toString_메서드_검증() {
		ArchRule rule = classes()
			.that().areAnnotatedWith(Entity.class)
			.and(new DescribedPredicate<>("양방향 연관관계가 있는 엔티티") {
				@Override
				public boolean test(JavaClass javaClass) {
					// OneToMany 또는 ManyToMany 관계가 있는지 확인
					return javaClass.getFields().stream()
						.anyMatch(field ->
							field.isAnnotatedWith(OneToMany.class) ||
								field.isAnnotatedWith(ManyToMany.class));
				}
			})
			.should(new ArchCondition<>("안전한 toString 메서드를 구현해야 함") {
				@Override
				public void check(JavaClass javaClass, ConditionEvents events) {
					// toString 메서드 확인
					boolean hasToString = javaClass.getMethods().stream()
						.anyMatch(method -> method.getName().equals("toString") &&
							method.getParameters().isEmpty());

					// @ToString 어노테이션 확인
					boolean hasToStringAnnotation = javaClass.getSourceCodeLocation().getSourceFileName() != null &&
						javaClass.getSourceCodeLocation().getSourceFileName().contains("@ToString");

					// @ToString(exclude = "...") 형태의 패턴 확인
					boolean hasExcludePattern = javaClass.getSourceCodeLocation().getSourceFileName() != null &&
						(javaClass.getSourceCodeLocation().getSourceFileName().contains("@ToString(exclude") ||
							javaClass.getSourceCodeLocation().getSourceFileName().contains("@ToString.Exclude"));

					boolean isSafe = !hasToString || hasExcludePattern || !hasToStringAnnotation;

					String message = String.format(
						"엔티티 클래스 '%s'는 %s",
						javaClass.getName(),
						isSafe ?
							"toString 메서드에서 순환 참조 문제를 방지하고 있습니다" :
							"toString 메서드에서 순환 참조 문제가 발생할 수 있습니다"
					);

					if (isSafe) {
						events.add(SimpleConditionEvent.satisfied(javaClass, message));
					} else {
						events.add(SimpleConditionEvent.violated(javaClass, message));
					}
				}
			})
			.because("양방향 연관관계가 있는 엔티티는 toString 메서드에서 순환 참조를 방지해야 합니다");

		rule.check(importedClasses);
	}

	/**
	 * 식별자 필드 등 불변 필드에 setter가 없는지 검증합니다.
	 */
	@Test
	public void 엔티티_불변_필드_검증() {
		ArchRule rule = classes()
			.that().areAnnotatedWith(Entity.class)
			.should(new ArchCondition<>("ID 필드에 setter가 없어야 함") {
				@Override
				public void check(JavaClass javaClass, ConditionEvents events) {
					// ID 필드 찾기
					JavaField idField = javaClass.getFields().stream()
						.filter(field -> field.isAnnotatedWith(Id.class) || field.isAnnotatedWith(EmbeddedId.class))
						.findFirst()
						.orElse(null);

					if (idField == null) {
						events.add(SimpleConditionEvent.satisfied(javaClass,
							String.format("엔티티 클래스 '%s'에 ID 필드가 없습니다", javaClass.getName())));
						return;
					}

					// ID 필드의 setter 메서드 확인
					String setterName = "set" + Character.toUpperCase(idField.getName().charAt(0)) +
						idField.getName().substring(1);
					boolean hasIdSetter = javaClass.getMethods().stream()
						.anyMatch(method -> method.getName().equals(setterName) &&
							method.getParameters().size() == 1);

					String message = String.format(
						"엔티티 클래스 '%s'의 ID 필드는 %s",
						javaClass.getName(),
						!hasIdSetter ? "setter가 없습니다" : "setter가 있습니다"
					);

					if (!hasIdSetter) {
						events.add(SimpleConditionEvent.satisfied(javaClass, message));
					} else {
						events.add(SimpleConditionEvent.violated(javaClass, message));
					}
				}
			})
			.because("엔티티의 식별자 필드는 불변이어야 하므로 setter를 가지지 않아야 합니다");

		rule.check(importedClasses);
	}

	/**
	 * 중요한 상태 변경이 명시적인 메서드로 캡슐화되어 있는지 검증합니다.
	 */
	@Test
	public void 비즈니스_메서드_캡슐화_검증() {
		ArchRule rule = classes()
			.that().areAnnotatedWith(Entity.class)
			.should(new ArchCondition<>("상태 변경이 명시적인 메서드로 캡슐화되어야 함") {
				@Override
				public void check(JavaClass javaClass, ConditionEvents events) {
					// setter 메서드 개수
					long setterCount = javaClass.getMethods().stream()
						.filter(method -> method.getName().startsWith("set") &&
							method.getParameters().size() == 1)
						.count();

					// 비즈니스 메서드 개수 (setter, getter가 아니면서 public인 메서드)
					long businessMethodCount = javaClass.getMethods().stream()
						.filter(method -> !method.getName().startsWith("set") &&
							!method.getName().startsWith("get") &&
							!method.getName().startsWith("is") &&
							method.getModifiers().contains(JavaModifier.PUBLIC))
						.count();

					// setter보다 비즈니스 메서드가 많거나 같으면 캡슐화가 잘 되어 있다고 판단
					boolean isWellEncapsulated = businessMethodCount >= setterCount;

					String message = String.format(
						"엔티티 클래스 '%s'는 %s (비즈니스 메서드: %d, setter: %d)",
						javaClass.getName(),
						isWellEncapsulated ?
							"상태 변경이 명시적인 메서드로 캡슐화되어 있습니다" :
							"상태 변경이 setter로 노출되어 있습니다",
						businessMethodCount,
						setterCount
					);

					if (isWellEncapsulated) {
						events.add(SimpleConditionEvent.satisfied(javaClass, message));
					} else {
						events.add(SimpleConditionEvent.violated(javaClass, message));
					}
				}
			})
			.because("엔티티의 중요한 상태 변경은 명시적인 비즈니스 메서드로 캡슐화되어야 합니다");

		rule.check(importedClasses);
	}

	/**
	 * 임베디드 값 객체가 불변인지 검증합니다.
	 */
	@Test
	public void 임베디드_값객체_불변성_검증() {
		ArchRule rule = classes()
			.that().areAnnotatedWith(Embeddable.class)
			.should(new ArchCondition<>("불변이어야 함") {
				@Override
				public void check(JavaClass javaClass, ConditionEvents events) {
					// setter 메서드 확인
					boolean hasSetters = javaClass.getMethods().stream()
						.anyMatch(method -> method.getName().startsWith("set") &&
							method.getParameters().size() == 1);

					// 필드가 final인지 확인
					boolean allFieldsFinal = javaClass.getFields().stream()
						.filter(field -> !field.getModifiers().contains(JavaModifier.STATIC))
						.allMatch(field -> field.getModifiers().contains(JavaModifier.FINAL));

					boolean isImmutable = !hasSetters || allFieldsFinal;

					String message = String.format(
						"임베디드 값 객체 '%s'는 %s",
						javaClass.getName(),
						isImmutable ? "불변입니다" : "불변이 아닙니다"
					);

					if (isImmutable) {
						events.add(SimpleConditionEvent.satisfied(javaClass, message));
					} else {
						events.add(SimpleConditionEvent.violated(javaClass, message));
					}
				}
			})
			.because("임베디드 값 객체는 불변이어야 합니다");

		rule.check(importedClasses);
	}

	/**
	 * Enum 타입 필드가 String으로 저장되도록 설정되어 있는지 검증합니다.
	 */
	@Test
	public void enum_타입_필드_처리_검증() {
		ArchRule rule = fields()
			.that().areDeclaredInClassesThat().areAnnotatedWith(Entity.class)
			.and(new DescribedPredicate<>("is enum type") {
				@Override
				public boolean test(JavaField field) {
					return field.getRawType().isEnum();
				}
			})
			.should().beAnnotatedWith(Enumerated.class)
			.andShould(new ArchCondition<>("@Enumerated(EnumType.STRING)로 선언되어야 함") {
				@Override
				public void check(JavaField field, ConditionEvents events) {
					if (!field.isAnnotatedWith(Enumerated.class)) {
						events.add(SimpleConditionEvent.violated(field,
							String.format("필드 '%s'는 @Enumerated 어노테이션이 없습니다", field.getFullName())));
						return;
					}

					Enumerated annotation = field.getAnnotationOfType(Enumerated.class);
					boolean isStringType = annotation.value().name().equals("STRING");

					String message = String.format(
						"Enum 타입 필드 '%s'는 %s",
						field.getFullName(),
						isStringType ?
							"@Enumerated(EnumType.STRING)로 선언되어 있습니다" :
							"@Enumerated(EnumType.STRING)로 선언되어 있지 않습니다"
					);

					if (isStringType) {
						events.add(SimpleConditionEvent.satisfied(field, message));
					} else {
						events.add(SimpleConditionEvent.violated(field, message));
					}
				}
			})
			.allowEmptyShould(true)  // 검사 대상이 없을 경우 성공으로 처리
			.because("Enum 타입 필드는 @Enumerated(EnumType.STRING)으로 선언하여 문자열로 저장해야 합니다");

		rule.check(importedClasses);
	}
}
