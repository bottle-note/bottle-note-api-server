package app.rule.data;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import app.rule.AbstractRules;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("rule")
@DisplayName("이벤트 페이로드 아키텍처 규칙")
@SuppressWarnings({"NonAsciiCharacters", "JUnitTestClassNamingConvention"})
public class EventPayloadRules extends AbstractRules {

  @Test
  public void 이벤트_페이로드_패키지_검증() {
    ArchRule rule =
        classes()
            .that()
            .haveSimpleNameEndingWith("Event")
            .should()
            .resideInAPackage("..event.payload..")
            .because("이벤트 객체는 event.payload 패키지에 위치해야 합니다");

    rule.check(importedClasses);
  }

  @Test
  public void 이벤트_페이로드_명명_규칙_검증() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("..event.payload..")
            .and()
            .areTopLevelClasses()
            .should()
            .haveSimpleNameEndingWith("Event")
            .because("event.payload 패키지의 모든 객체는 Event 로 끝나야 합니다");

    rule.check(importedClasses);
  }

  @Test
  public void 이벤트_페이로드_레코드_타입_검증() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("..event.payload..")
            .and()
            .areTopLevelClasses()
            .should()
            .beRecords()
            .because("event.payload 패키지의 모든 객체는 record 타입이어야 합니다");

    rule.check(importedClasses);
  }
}
