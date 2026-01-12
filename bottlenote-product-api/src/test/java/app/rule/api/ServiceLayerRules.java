package app.rule.api;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import app.bottlenote.common.annotation.FacadeService;
import app.bottlenote.common.annotation.ThirdPartyService;
import app.rule.AbstractRules;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import lombok.Getter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 서비스 계층의 아키텍처 규칙을 검증하는 테스트 클래스입니다. */
@Tag("rule")
@DisplayName("서비스 비지니스 레이어 아키텍처 규칙")
@SuppressWarnings({"NonAsciiCharacters", "JUnitTestClassNamingConvention"})
public class ServiceLayerRules extends AbstractRules {
  /** 서비스 클래스 명명 규칙을 검증합니다. 모든 서비스 클래스는 이름이 'Service'로 끝나야 합니다. */
  @Test
  public void 서비스_클래스_명명_규칙_검증() {
    ArchRule rule =
        classes()
            .that()
            .areAnnotatedWith(Service.class)
            .should()
            .haveSimpleNameEndingWith("Service")
            .because("서비스 클래스는 명확한 식별을 위해 'Service'로 끝나야 합니다");

    rule.check(importedClasses);
  }

  /** 서비스 패키지 구조를 검증합니다. 모든 서비스 클래스는 '.service' 패키지에 위치해야 합니다. */
  @Test
  public void 서비스_패키지_구조_검증() {
    ArchRule rule =
        classes()
            .that()
            .areAnnotatedWith(Service.class)
            .and()
            .areNotAnnotations() // 어노테이션 타입 자체는 제외
            .should()
            .resideInAPackage("..service..")
            .because("서비스 클래스는 구조적 일관성을 위해 '.service' 패키지에 위치해야 합니다");

    rule.check(importedClasses);
  }

  /** 서비스 계층 의존성 방향을 검증합니다. 서비스는 컨트롤러에 의존해서는 안 됩니다. */
  @Test
  public void 서비스_계층_의존성_검증() {
    ArchRule rule =
        noClasses()
            .that()
            .areAnnotatedWith(Service.class)
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..controller..")
            .because("서비스는 컨트롤러에 의존해서는 안 됩니다 (계층 아키텍처 원칙 위반)");

    rule.check(importedClasses);
  }

  /** 서비스의 모든 public 메서드에 트랜잭션 애노테이션이 적용되어 있는지 검증합니다. 트랜잭션 경계는 서비스 계층의 public 메서드에서 정의되어야 합니다. */
  @Test
  public void 서비스_public_메서드_트랜잭션_검증() {
    ArchRule rule =
        methods()
            .that()
            .areDeclaredInClassesThat()
            .areAnnotatedWith(Service.class)
            .and()
            .areDeclaredInClassesThat()
            .areNotAnnotatedWith(FacadeService.class)
            .and()
            .areDeclaredInClassesThat()
            .areNotAnnotatedWith(ThirdPartyService.class)
            .and()
            .arePublic()
            .and()
            .areNotStatic()
            .and()
            .areNotDeclaredIn(FacadeService.class)
            .and()
            .areNotDeclaredIn(ThirdPartyService.class)
            .should()
            .beAnnotatedWith(Transactional.class)
            .because("서비스의 모든 public 메서드는 트랜잭션 경계를 명확히 하기 위해 @Transactional 애노테이션을 가져야 합니다");

    rule.check(importedClasses);
  }

  /** 서비스의 비-public 메서드에는 트랜잭션 애노테이션이 없어야 함을 검증합니다. 트랜잭션은 public 인터페이스에서만 정의되어야 합니다. */
  @Test
  public void 서비스_비public_메서드_트랜잭션_금지_검증() {
    ArchRule rule =
        methods()
            .that()
            .areDeclaredInClassesThat()
            .areAnnotatedWith(Service.class)
            .and()
            .areNotPublic()
            .should()
            .notBeAnnotatedWith(Transactional.class)
            .because(
                "서비스의 비-public 메서드에는 @Transactional 애노테이션을 사용하지 않아야 합니다. 트랜잭션은 public 인터페이스에서만 정의되어야 합니다");

    rule.check(importedClasses);
  }

  /** 서비스 클래스에서 필드 주입 사용을 금지합니다. 서비스에서는 생성자 주입 방식을 사용해야 합니다. */
  @Test
  public void 필드_주입_금지_검증() {
    ArchRule rule =
        noFields()
            .that()
            .areDeclaredInClassesThat()
            .areAnnotatedWith(Service.class)
            .should()
            .beAnnotatedWith(Autowired.class)
            .because("필드 주입(@Autowired)은 금지되어 있으며, 생성자 주입을 사용해야 합니다");

    rule.check(importedClasses);
  }

  /** 서비스 메서드 카멜케이스 검증. */
  @Test
  public void 서비스_메서드_카멜케이스_검증() {
    ArchRule rule =
        methods()
            .that()
            .areDeclaredInClassesThat()
            .areAnnotatedWith(Service.class)
            .and()
            .arePublic()
            .should()
            .haveNameMatching("^[a-z][a-zA-Z0-9]*$")
            .because("서비스 메서드는 카멜케이스 명명 규칙을 따라야 합니다");

    rule.check(importedClasses);
  }

  /** 서비스 메서드의 매개변수 개수를 검증합니다. 너무 많은 매개변수는 메서드 호출을 복잡하게 만들고 가독성을 저하시킵니다. */
  @Test
  public void 서비스_메서드_매개변수_개수_제한_검증() {
    ArchRule rule =
        methods()
            .that()
            .areDeclaredInClassesThat()
            .areAnnotatedWith(Service.class)
            .should(
                new ArchCondition<JavaMethod>("have parameter count less than or equal to 5") {
                  @Override
                  public void check(JavaMethod method, ConditionEvents events) {
                    int paramCount = method.getParameters().size();
                    boolean satisfied = paramCount <= 5;

                    String message =
                        String.format(
                            "메서드 '%s'는 매개변수 %d개로, 5개 제한을 %s",
                            method.getFullName(), paramCount, satisfied ? "준수합니다" : "위반합니다");

                    if (satisfied) {
                      events.add(SimpleConditionEvent.satisfied(method, message));
                    } else {
                      events.add(SimpleConditionEvent.violated(method, message));
                    }
                  }
                })
            .because("서비스 메서드는 최대 5개까지 매개변수를 가질 수 있습니다. 6개 이상의 매개변수는 DTO로 래핑해야 합니다");

    rule.check(importedClasses);
  }

  /** 서비스 클래스의 public 메서드 개수를 검증합니다. 너무 많은 public 메서드는 클래스가 여러 책임을 가질 가능성이 높습니다. */
  @Test
  public void 서비스_클래스_크기_제한_검증() {
    ArchRule rule =
        classes()
            .that()
            .areAnnotatedWith(Service.class)
            .should(
                new ArchCondition<JavaClass>("have less than or equal to 15 public methods") {
                  @Override
                  public void check(JavaClass javaClass, ConditionEvents events) {
                    long publicMethodCount =
                        javaClass.getMethods().stream()
                            .filter(method -> method.getModifiers().contains(JavaModifier.PUBLIC))
                            .count();

                    boolean satisfied = publicMethodCount <= 15;
                    String message =
                        String.format(
                            "서비스 클래스 '%s'는 %d개의 public 메서드를 가지고 있어 최대 15개 제한을 %s",
                            javaClass.getName(), publicMethodCount, satisfied ? "준수합니다" : "위반합니다");

                    if (satisfied) {
                      events.add(SimpleConditionEvent.satisfied(javaClass, message));
                    } else {
                      events.add(SimpleConditionEvent.violated(javaClass, message));
                    }
                  }
                })
            .because("서비스 클래스는 최대 15개의 public 메서드를 가져야 합니다. 그 이상은 책임을 분리해야 함을 의미합니다");

    rule.check(importedClasses);
  }

  /** 비동기 메서드의 반환 타입을 검증합니다. @Async 메서드는 void, Future 또는 CompletableFuture를 반환해야 합니다. */
  @Test
  public void 비동기_메서드_반환타입_검증() {
    ArchRule rule =
        methods()
            .that()
            .areAnnotatedWith(Async.class)
            .should(
                new ArchCondition<JavaMethod>("return void, Future or CompletableFuture") {
                  @Override
                  public void check(JavaMethod method, ConditionEvents events) {
                    String returnTypeName = method.getReturnType().getName();
                    boolean satisfied =
                        returnTypeName.equals("void")
                            || returnTypeName.endsWith("Future")
                            || returnTypeName.contains("CompletableFuture");

                    String message =
                        String.format(
                            "비동기 메서드 '%s'는 반환 타입이 '%s'로, 요구사항을 %s",
                            method.getFullName(),
                            returnTypeName,
                            satisfied
                                ? "준수합니다"
                                : "위반합니다 (void, Future, CompletableFuture 중 하나여야 함)");

                    if (satisfied) {
                      events.add(SimpleConditionEvent.satisfied(method, message));
                    } else {
                      events.add(SimpleConditionEvent.violated(method, message));
                    }
                  }
                })
            .because("@Async 메서드는 void, Future 또는 CompletableFuture 타입을 반환해야 합니다");

    rule.check(importedClasses);
  }

  /** 서비스 계층의 의존성 방향을 검증합니다. 서비스는 퍼사드 서비스, 써드파티 서비스, 리포지토리에만 의존해야 합니다. */
  @Test
  public void 서비스_의존성_방향_상세_검증() {
    ArchRule rule =
        classes()
            .that()
            .areAnnotatedWith(Service.class)
            .and(
                new DescribedPredicate<>("are not facade or third-party services") {
                  @Override
                  public boolean test(JavaClass javaClass) {
                    return !javaClass.isAnnotatedWith(FacadeService.class)
                        && !javaClass.isAnnotatedWith(ThirdPartyService.class);
                  }
                })
            .should()
            .onlyDependOnClassesThat(
                new DescribedPredicate<>("are allowed dependencies") {
                  @Override
                  public boolean test(JavaClass dependency) {
                    String packageName = dependency.getPackageName();
                    return AllowedPackageType.isAllowed(packageName);
                  }
                })
            .because("서비스 클래스는 퍼사드 서비스, 써드파티 서비스, 리포지토리에만 의존해야 합니다");

    rule.check(importedClasses);
  }

  /** 서비스 계층의 의존성 검증을 위한 허용 패키지 정의 */
  @Getter
  enum AllowedPackageType {
    // 표준 라이브러리 패키지 (HIGH: 필수적으로 허용되어야 하는 패키지)
    JAVA("표준 라이브러리", "java."),
    JAKARTA("Jakarta EE", "jakarta."),
    SPRING("Spring 프레임워크", "org.springframework."),
    LOMBOK("Lombok 라이브러리", "lombok."),
    SLF4J("로깅 라이브러리", "org.slf4j"),
    APACHE_COMMONS("Apache Commons 라이브러리", "org.apache.commons."),
    AHOCORASICK("Aho-Corasick 라이브러리", "org.ahocorasick."),

    // 애플리케이션 계층 패키지 (MEDIUM: 아키텍처에 따라 허용되는 패키지)
    SERVICE("서비스 계층", ".service."),
    REPOSITORY("리포지토리 계층", ".repository."),
    DOMAIN("도메인 계층", ".domain."),
    DTO("데이터 전송 객체", ".dto."),
    COMMON("공통 유틸리티", ".common."),
    GLOBAL("전역 설정", ".global."),

    // 애플리케이션 루트 패키지 (LOW: 조건부로 허용될 수 있는 패키지)
    APP_ROOT("애플리케이션 루트", "app.bottlenote.");

    private final String type;
    private final String packagePrefix;

    AllowedPackageType(String type, String packagePrefix) {
      this.type = type;
      this.packagePrefix = packagePrefix;
    }

    /** 주어진 패키지 이름이 허용된 패키지인지 확인합니다. */
    public static boolean isAllowed(String packageName) {
      // 컨트롤러 패키지는 명시적으로 불허용
      if (packageName.contains(".controller.")) {
        return false;
      }

      // 높은 중요도 패키지부터 확인
      for (AllowedPackageType type : values()) {
        if (type.matches(packageName)) {
          return true;
        }
      }

      return false;
    }

    public boolean matches(String packageName) {
      // 패키지 접두사인지 또는 포함되어 있는지에 따라 다른 매칭 로직 적용
      if (this == JAVA
          || this == JAKARTA
          || this == SPRING
          || this == LOMBOK
          || this == SLF4J
          || this == APP_ROOT) {
        return packageName.startsWith(packagePrefix);
      } else {
        return packageName.contains(packagePrefix);
      }
    }
  }
}
