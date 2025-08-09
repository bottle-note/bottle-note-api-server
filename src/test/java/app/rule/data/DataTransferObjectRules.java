package app.rule.data;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import app.rule.AbstractRules;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("rule")
@DisplayName("데이터 전송 객체(DTO) 아키텍처 규칙")
@SuppressWarnings({"NonAsciiCharacters", "JUnitTestClassNamingConvention"})
public class DataTransferObjectRules extends AbstractRules {
  /** 요청 DTO 네이밍 규칙 검증 요청 DTO 클래스는 이름이 'Request'로 끝나야 합니다. */
  @Test
  public void 요청_DTO_네이밍_규칙_검증() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("..dto.request..")
            .and()
            .areNotNestedClasses()
            .and()
            .haveNameNotMatching(".*\\$.*")
            .and()
            .areTopLevelClasses()
            .should()
            .haveSimpleNameEndingWith("Request")
            .orShould()
            .haveSimpleNameContaining("Item")
            .because("요청 DTO 클래스는 명확한 식별을 위해 'Request'로 끝나야 합니다");

    rule.check(importedClasses);
  }

  /** 응답 DTO 네이밍 규칙 검증 응답 DTO 클래스는 이름이 'Response'로 끝나야 합니다. */
  @Test
  public void 응답_DTO_네이밍_규칙_검증() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("..dto.response..")
            .and()
            .areNotNestedClasses()
            .and()
            .haveNameNotMatching(".*\\$.*")
            .and()
            .areTopLevelClasses()
            .should()
            .haveSimpleNameEndingWith("Response")
            .orShould()
            .haveSimpleNameContaining("Item")
            .because("응답 DTO 클래스는 명확한 식별을 위해 'Response' , 'Item'로 끝나야 합니다");

    rule.check(importedClasses);
  }

  @Test
  public void 조회용_객체_규칙_검증() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("..dto.dsl..")
            .and()
            .areNotNestedClasses()
            .and()
            .haveNameNotMatching(".*\\$.*")
            .and()
            .areTopLevelClasses()
            .should()
            .haveSimpleNameEndingWith("Criteria")
            .because("조회용 객체는 조회용 객체를 의미하는 'Criteria'로 끝나야 합니다");
  }

  /** dto 폴더 아래에는 request, response, dsl 폴더만 존재해야 합니다. */
  @Test
  public void dto_패키지_구조_검증() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("..dto..")
            .should()
            .resideInAPackage("..dto.request..")
            .orShould()
            .resideInAPackage("..dto.response..")
            .orShould()
            .resideInAPackage("..dto.dsl..")
            .because("dto 패키지 아래에는 request, response, dsl 폴더만 존재해야 합니다");

    rule.check(importedClasses);
  }

  /** 검색 조건 DTO 네이밍 규칙 검증 검색 조건 DTO 클래스는 이름이 'Criteria'로 끝나야 합니다. */
  @Test
  public void 검색_조건_DTO_네이밍_규칙_검증() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("..dto.dsl..")
            .should()
            .haveSimpleNameEndingWith("Criteria")
            .because("검색 조건 DTO 클래스는 명확한 식별을 위해 'Criteria'로 끝나야 합니다");

    rule.check(importedClasses);
  }

  /** 요청 DTO 패키지 위치 검증 모든 요청 DTO 클래스는 '.dto.request' 패키지에 위치해야 합니다. */
  @Test
  public void 요청_DTO_패키지_위치_검증() {
    ArchRule rule =
        classes()
            .that()
            .haveSimpleNameEndingWith("Request")
            .and()
            .doNotHaveSimpleName("GlobalResponse")
            .and()
            .doNotHaveSimpleName("PageResponse")
            .should()
            .resideInAPackage("..dto.request..")
            .because("요청 DTO 클래스는 구조적 일관성을 위해 '.dto.request' 패키지에 위치해야 합니다");

    rule.check(importedClasses);
  }

  /** 응답 DTO 패키지 위치 검증 모든 응답 DTO 클래스는 '.dto.response' 패키지에 위치해야 합니다. */
  @Test
  public void 응답_DTO_패키지_위치_검증() {
    ArchRule rule =
        classes()
            .that()
            .haveSimpleNameEndingWith("Response")
            .and()
            .doNotHaveSimpleName("GlobalResponse")
            .and()
            .doNotHaveSimpleName("PageResponse")
            .and()
            .doNotHaveSimpleName("CursorResponse")
            .and()
            .doNotHaveSimpleName("CollectionResponse")
            .should()
            .resideInAPackage("..dto.response..")
            .because("응답 DTO 클래스는 구조적 일관성을 위해 '.dto.response' 패키지에 위치해야 합니다");

    rule.check(importedClasses);
  }

  /** 검색 조건 DTO 패키지 위치 검증 모든 검색 조건 DTO 클래스는 '.dto.dsl' 패키지에 위치해야 합니다. */
  @Test
  public void 검색_조건_DTO_패키지_위치_검증() {
    ArchRule rule =
        classes()
            .that()
            .haveSimpleNameEndingWith("Criteria")
            .or()
            .haveSimpleNameEndingWith("criteria")
            .should()
            .resideInAPackage("..dto.dsl..")
            .because("검색 조건 DTO 클래스는 구조적 일관성을 위해 '.dto.dsl' 패키지에 위치해야 합니다");

    rule.check(importedClasses);
  }

  /** DTO-엔티티 분리 검증 DTO 클래스는 엔티티를 직접 참조하지 않아야 합니다. */
  @Test
  public void DTO_엔티티_분리_검증() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAnyPackage("..dto..")
            .should()
            .dependOnClassesThat()
            .areAnnotatedWith(Entity.class)
            .because("DTO 클래스는 엔티티를 직접 참조하지 않아야 합니다");
    rule.check(importedClasses);
  }
}
