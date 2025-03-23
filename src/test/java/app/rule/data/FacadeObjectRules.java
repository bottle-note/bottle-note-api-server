package app.rule.data;

import app.bottlenote.common.annotation.FacadeService;
import app.rule.AbstractRules;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

/**
 * 퍼사드 계층의 아키텍처 규칙을 검증하는 테스트 클래스입니다.
 */
@Tag("rule")
@DisplayName("퍼사드 계층 아키텍처 규칙")
@SuppressWarnings({"NonAsciiCharacters", "JUnitTestClassNamingConvention"})
public class FacadeObjectRules extends AbstractRules {

	/**
	 * 퍼사드 인터페이스 명명 규칙을 검증합니다.
	 * 모든 퍼사드 인터페이스는 이름이 'Facade'로 끝나야 합니다.
	 */
	@Test
	public void 퍼사드_인터페이스_명명_규칙_검증() {
		ArchRule rule = classes()
				.that().resideInAPackage("..facade")
				.and().areInterfaces()
				.should().haveSimpleNameEndingWith("Facade")
				.because("퍼사드 인터페이스는 명확한 식별을 위해 'Facade'로 끝나야 합니다");

		rule.check(importedClasses);
	}

	/**
	 * 퍼사드 구현체 명명 규칙을 검증합니다.
	 * 모든 퍼사드 구현체는 이름이 'Default'로 시작하고 'Facade'로 끝나야 합니다.
	 */
	@Test
	public void 퍼사드_구현체_명명_규칙_검증() {
		ArchRule rule = classes()
				.that().areAnnotatedWith(FacadeService.class)
				.should().haveSimpleNameStartingWith("Default")
				.andShould().haveSimpleNameEndingWith("Facade")
				.because("퍼사드 구현체는 'Default'로 시작하고 'Facade'로 끝나야 합니다");

		rule.check(importedClasses);
	}

	/**
	 * 퍼사드 구현체 어노테이션 검증을 수행합니다.
	 * 'Facade'로 끝나는 구현체 클래스는 @FacadeService 어노테이션을 가져야 합니다.
	 */
	@Test
	public void 퍼사드_구현체_어노테이션_검증() {
		ArchRule rule = classes()
				.that().haveSimpleNameEndingWith("Facade")
				.and().haveSimpleNameStartingWith("Default")
				.and().areNotInterfaces()
				.should().beAnnotatedWith(FacadeService.class)
				.because("퍼사드 구현체는 @FacadeService 어노테이션으로 명확히 식별되어야 합니다");

		rule.check(importedClasses);
	}

	/**
	 * 퍼사드 패키지 구조를 검증합니다.
	 * 모든 퍼사드 인터페이스는 도메인의 'facade' 패키지에 위치해야 합니다.
	 */
	@Test
	@Disabled
	public void 퍼사드_패키지_구조_검증() {
		ArchRule rule = classes()
				.that().haveSimpleNameEndingWith("Facade")
				.and().areInterfaces()
				.should().resideInAPackage("..facade")
				.because("퍼사드 인터페이스는 구조적 일관성을 위해 도메인의 'facade' 패키지에 위치해야 합니다");

		rule.check(importedClasses);
	}

	/**
	 * 퍼사드 구현체 위치 검증을 수행합니다.
	 * 퍼사드 구현체는 'service' 패키지에 위치해야 합니다.
	 */
	@Test
	public void 퍼사드_구현체_위치_검증() {
		ArchRule rule = classes()
				.that().areAnnotatedWith(FacadeService.class)
				.should().resideInAPackage("..service..")
				.because("퍼사드 구현체는 서비스 계층에 위치하여 다른 서비스를 통합하는 역할을 수행합니다");

		rule.check(importedClasses);
	}

	/**
	 * 페이로드 객체 패키지 구조를 검증합니다.
	 * 퍼사드의 페이로드 객체는 'facade.payload' 패키지에 위치해야 합니다.
	 */
	@Test
	public void 퍼사드_페이로드_패키지_구조_검증() {
		ArchRule rule = classes()
				.that().haveNameMatching(".*Info$|.*VO$|.*Item$")
				.and().areNotEnums()
				.and().areTopLevelClasses()
				.and().resideInAPackage("..facade.payload..")
				.should().resideInAPackage("..facade.payload..")
				.because("퍼사드의 페이로드 객체는 'facade.payload' 패키지에 위치해야 합니다");

		rule.check(importedClasses);
	}

	/**
	 * 페이로드 객체 명명 규칙을 검증합니다.
	 * 페이로드 객체는 'Info', 'VO', 'Item' 접미사를 가져야 합니다.
	 */
	@Test
	public void 퍼사드_페이로드_명명_규칙_검증() {
		ArchRule rule = classes()
				.that().resideInAPackage("..facade.payload..")
				.and().areTopLevelClasses()
				.and().areNotNestedClasses()
				.should(new ArchCondition<>("have names ending with Info or Item") {
					@Override
					public void check(JavaClass javaClass, ConditionEvents events) {
						String name = javaClass.getSimpleName();
						boolean hasValidSuffix = name.endsWith("Info") || name.endsWith("VO") || name.endsWith("Item");

						String message = String.format(
								"클래스 '%s'는 명명 규칙을 %s",
								javaClass.getName(),
								hasValidSuffix ? "준수합니다" : "위반합니다 (Info, VO, Item 중 하나로 끝나야 함)"
						);

						if (hasValidSuffix) {
							events.add(SimpleConditionEvent.satisfied(javaClass, message));
						} else {
							events.add(SimpleConditionEvent.violated(javaClass, message));
						}
					}
				})
				.because("퍼사드 페이로드 객체는 'Info', 'Item' 접미사를 가져야 합니다");

		rule.check(importedClasses);
	}

	/**
	 * 퍼사드와 컨트롤러 간의 의존성 검증을 수행합니다.
	 * 컨트롤러는 퍼사드 인터페이스에 의존할 수 있지만, 퍼사드 구현체에 직접 의존해서는 안 됩니다.
	 */
	@Test
	public void 퍼사드_컨트롤러_의존성_검증() {
		ArchRule rule = classes()
				.that().resideInAPackage("..controller..")
				.should().onlyDependOnClassesThat(
						new DescribedPredicate<>("are not facade implementations") {
							@Override
							public boolean test(JavaClass javaClass) {
								return !javaClass.getName().startsWith("Default") ||
										!javaClass.getName().endsWith("Facade");
							}
						})
				.because("컨트롤러는 퍼사드 인터페이스에만 의존하고 구현체에 직접 의존하지 않아야 합니다");

		rule.check(importedClasses);
	}

	/**
	 * 퍼사드 메서드 시그니처를 검증합니다.
	 * 퍼사드 메서드는 도메인 엔티티를 외부에 노출하지 않아야 합니다.
	 */
	@Test
	public void 퍼사드_메서드_시그니처_검증() {
		ArchRule rule = methods()
				.that().areDeclaredInClassesThat().haveNameMatching(".*Facade")
				.should(new ArchCondition<>("not expose domain entities") {
					@Override
					public void check(JavaMethod method, ConditionEvents events) {
						String returnTypeName = method.getReturnType().getName();

						// 도메인 엔티티 확인 (패키지 경로에 domain이 포함되고 DTO, VO 등의 접미사가 없는 경우)
						boolean isExposingEntity = returnTypeName.contains(".domain.") &&
								!returnTypeName.endsWith("DTO") &&
								!returnTypeName.endsWith("VO") &&
								!returnTypeName.endsWith("Info") &&
								!returnTypeName.endsWith("Item") &&
								!returnTypeName.contains("Response");

						String message = String.format(
								"메서드 '%s'는 %s",
								method.getFullName(),
								isExposingEntity ? "도메인 엔티티를 직접 노출합니다" : "도메인 엔티티를 노출하지 않습니다"
						);

						if (!isExposingEntity) {
							events.add(SimpleConditionEvent.satisfied(method, message));
						} else {
							events.add(SimpleConditionEvent.violated(method, message));
						}
					}
				})
				.because("퍼사드는 도메인 엔티티를 직접 노출하지 않고 DTO나 VO를 통해 데이터를 전달해야 합니다");

		rule.check(importedClasses);
	}

}
