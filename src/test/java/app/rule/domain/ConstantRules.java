package app.rule.domain;

import app.rule.AbstractRules;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@Tag("rule")
@DisplayName("도메인 상수 아키텍처 규칙")
@SuppressWarnings({"NonAsciiCharacters", "JUnitTestClassNamingConvention"})
public class ConstantRules extends AbstractRules {

	/**
	 * 모든 도메인 상수 클래스는 '**.domain.constant' 패키지에 위치해야 합니다.
	 */
	@Test
	@Disabled
	public void 도메인_상수_패키지_위치_검증() {
		ArchRule rule = classes()
			.that().areEnums()
			.and().haveSimpleNameNotContaining("Exception")
			.and().areNotInnerClasses()
			.and().areNotAnnotations()
			.and().areNotInterfaces()
			.and().areNotAnonymousClasses()
			.should().resideInAPackage("..domain.constant..")
			.because("모든 엔티티 클래스는 '**.domain.constant' 패키지에 위치해야 합니다");

		rule.check(importedClasses);
	}
}
