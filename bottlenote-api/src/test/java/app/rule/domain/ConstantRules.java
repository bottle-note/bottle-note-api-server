package app.rule.domain;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import app.bottlenote.global.annotation.ExcludeRule;
import app.rule.AbstractRules;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("rule")
@DisplayName("상수 아키텍처 규칙")
@SuppressWarnings({"NonAsciiCharacters", "JUnitTestClassNamingConvention"})
public class ConstantRules extends AbstractRules {

  /** 모든 도메인 상수 클래스는 '**.domain.constant' 패키지에 위치해야 합니다. @ExcludeRule 어노테이션이 붙은 클래스는 규칙에서 제외됩니다. */
  @Test
  public void 상수_패키지_위치_검증() {
    ArchRule rule =
        classes()
            .that()
            .areEnums()
            .and()
            .haveSimpleNameNotContaining("Exception")
            .and()
            .areNotInnerClasses()
            .and()
            .haveNameNotMatching(".*\\$.*")
            .and()
            .areNotAnnotatedWith(ExcludeRule.class)
            .and()
            .areNotAnnotations()
            .and()
            .areNotInterfaces()
            .and()
            .areNotAnonymousClasses()
            .should()
            .resideInAPackage("..constant..")
            .because("모든 독립적인 열거형 클래스는 '**.constant' 패키지에 위치해야 합니다");

    rule.check(importedClasses);
  }
}
