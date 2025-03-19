package app.rule.api;

import app.rule.AbstractRules;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

/**
 * 서비스 계층의 아키텍처 규칙을 검증하는 테스트 클래스입니다.
 */
@Tag("rule")
@DisplayName("서비스 비지니스 레이어 아키텍처 규칙")
@SuppressWarnings({"NonAsciiCharacters", "JUnitTestClassNamingConvention"})
public class ServiceLayerRules extends AbstractRules {
	/**
	 * 서비스 클래스 명명 규칙을 검증합니다.
	 * 모든 서비스 클래스는 이름이 'Service'로 끝나야 합니다.
	 */
	@Test
	public void 서비스_클래스_명명_규칙_검증() {
		ArchRule rule = classes()
			.that().areAnnotatedWith(Service.class)
			.should().haveSimpleNameEndingWith("Service")
			.because("서비스 클래스는 명확한 식별을 위해 'Service'로 끝나야 합니다");

		rule.check(importedClasses);
	}

	/**
	 * 서비스 패키지 구조를 검증합니다.
	 * 모든 서비스 클래스는 '.service' 패키지에 위치해야 합니다.
	 */
	@Test
	public void 서비스_패키지_구조_검증() {
		ArchRule rule = classes()
			.that().areAnnotatedWith(Service.class)
			.should().resideInAPackage("..service..")
			.because("서비스 클래스는 구조적 일관성을 위해 '.service' 패키지에 위치해야 합니다");

		rule.check(importedClasses);
	}

	/**
	 * 서비스 계층 의존성 방향을 검증합니다.
	 * 서비스는 컨트롤러에 의존해서는 안 됩니다.
	 */
	@Test
	public void 서비스_계층_의존성_검증() {
		ArchRule rule = noClasses()
			.that().areAnnotatedWith(Service.class)
			.should().dependOnClassesThat().resideInAPackage("..controller..")
			.because("서비스는 컨트롤러에 의존해서는 안 됩니다 (계층 아키텍처 원칙 위반)");

		rule.check(importedClasses);
	}

	/**
	 * 서비스의 모든 public 메서드에 트랜잭션 애노테이션이 적용되어 있는지 검증합니다.
	 * 트랜잭션 경계는 서비스 계층의 public 메서드에서 정의되어야 합니다.
	 */
	@Test
	public void 서비스_public_메서드_트랜잭션_검증() {
		ArchRule rule = methods()
			.that().areDeclaredInClassesThat().areAnnotatedWith(Service.class)
			.and().arePublic()
			.and().areNotStatic()
			.should().beAnnotatedWith(Transactional.class)
			.because("서비스의 모든 public 메서드는 트랜잭션 경계를 명확히 하기 위해 @Transactional 애노테이션을 가져야 합니다");

		rule.check(importedClasses);
	}

	/**
	 * 서비스의 비-public 메서드에는 트랜잭션 애노테이션이 없어야 함을 검증합니다.
	 * 트랜잭션은 public 인터페이스에서만 정의되어야 합니다.
	 */
	@Test
	public void 서비스_비public_메서드_트랜잭션_금지_검증() {
		ArchRule rule = methods()
			.that().areDeclaredInClassesThat().areAnnotatedWith(Service.class)
			.and().areNotPublic()
			.should().notBeAnnotatedWith(Transactional.class)
			.because("서비스의 비-public 메서드에는 @Transactional 애노테이션을 사용하지 않아야 합니다. 트랜잭션은 public 인터페이스에서만 정의되어야 합니다");

		rule.check(importedClasses);
	}

	/**
	 * 서비스 클래스에서 필드 주입 사용을 금지합니다.
	 * 서비스에서는 생성자 주입 방식을 사용해야 합니다.
	 */
	@Test
	public void 필드_주입_금지_검증() {
		ArchRule rule = noFields()
			.that().areDeclaredInClassesThat().areAnnotatedWith(Service.class)
			.should().beAnnotatedWith(Autowired.class)
			.because("필드 주입(@Autowired)은 금지되어 있으며, 생성자 주입을 사용해야 합니다");

		rule.check(importedClasses);
	}

	/**
	 * 서비스 메서드의 명명 규칙을 검증합니다.
	 * 메서드 이름은 수행하는 작업을 명확하게 반영해야 합니다.
	 */
	@Test
	public void 서비스_메서드_명명_규칙_검증() {
		ArchRule rule = methods()
			.that().areDeclaredInClassesThat().areAnnotatedWith(Service.class)
			.and().arePublic()
			.should(followServiceMethodNamingConvention())
			.because("서비스 메서드는 명확한 동사로 시작하는 명명 규칙을 따라야 합니다(예: getUserById, createOrder)");

		rule.check(importedClasses);
	}

	/**
	 * 서비스 메서드가 명명 규칙을 따르는지 확인하는 커스텀 조건입니다.
	 */
	private ArchCondition<JavaMethod> followServiceMethodNamingConvention() {
		return new ArchCondition<>("서비스 메서드 명명 규칙을 따름") {
			@Override
			public void check(JavaMethod method, ConditionEvents events) {
				String methodName = method.getName();
				// 허용되는 동사 접두사 목록
				String[] allowedPrefixes = {"get", "find", "retrieve", "load", "create", "save", "update",
					"delete", "remove", "process", "compute", "calculate", "validate",
					"check", "exists", "is", "has"};

				boolean startsWithVerb = Arrays.stream(allowedPrefixes)
					.anyMatch(methodName::startsWith);

				// camelCase 확인
				boolean isCamelCase = Character.isLowerCase(methodName.charAt(0)) &&
					!methodName.contains("_") &&
					methodName.matches("^[a-z][a-zA-Z0-9]*$");

				if (!startsWithVerb || !isCamelCase) {
					events.add(SimpleConditionEvent.violated(
						method,
						"메서드 이름 '" + methodName + "'은(는) 동사로 시작하는 camelCase 형태가 아닙니다"
					));
				} else {
					events.add(SimpleConditionEvent.satisfied(method, "메서드 이름이 명명 규칙을 따릅니다"));
				}
			}
		};
	}
}
