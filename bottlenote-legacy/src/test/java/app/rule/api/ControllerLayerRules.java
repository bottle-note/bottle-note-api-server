package app.rule.api;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import app.rule.AbstractRules;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 컨트롤러 계층의 아키텍처 규칙을 검증하는 테스트 클래스입니다. */
@Tag("rule")
@DisplayName("API 컨트롤러 레이어 아키텍처 규칙")
@SuppressWarnings({"NonAsciiCharacters", "JUnitTestClassNamingConvention"})
public class ControllerLayerRules extends AbstractRules {

  /** 컨트롤러 클래스 명명 규칙을 검증합니다. 컨트롤러 애노테이션이 있는 모든 클래스는 이름이 'Controller'로 끝나야 합니다. */
  @Test
  public void 컨트롤러_클래스_명명_규칙_검증() {
    ArchRule rule =
        classes()
            .that()
            .areAnnotatedWith(RestController.class)
            .or()
            .areAnnotatedWith(Controller.class)
            .should()
            .haveSimpleNameEndingWith("Controller")
            .because("컨트롤러 클래스는 명확한 식별을 위해 'Controller'로 끝나야 합니다");

    rule.check(importedClasses);
  }

  /** 컨트롤러 패키지 구조를 검증합니다. 모든 컨트롤러 클래스는 '.controller' 또는 '.api' 패키지에 위치해야 합니다. */
  @Test
  public void 컨트롤러_패키지_구조_검증() {
    ArchRule rule =
        classes()
            .that()
            .areAnnotatedWith(RestController.class)
            .or()
            .areAnnotatedWith(Controller.class)
            .should()
            .resideInAnyPackage("..controller..", "..api..")
            .because("컨트롤러 클래스는 구조적 일관성을 위해 '.controller' 또는 '.api' 패키지에 위치해야 합니다");

    rule.check(importedClasses);
  }

  /**
   * REST 애노테이션 검증을 수행합니다. 'Controller'로 끝나는 모든 클래스는 @RestController 또는 @Controller 애노테이션을 가져야 합니다.
   */
  @Test
  public void REST_애노테이션_검증() {
    ArchRule rule =
        classes()
            .that()
            .haveSimpleNameEndingWith("Controller")
            .should()
            .beAnnotatedWith(RestController.class)
            .orShould()
            .beAnnotatedWith(Controller.class)
            .because("컨트롤러 클래스는 @RestController 또는 @Controller 애노테이션으로 명확히 식별되어야 합니다");

    rule.check(importedClasses);
  }

  /** 요청 매핑 검증을 수행합니다. 모든 컨트롤러 클래스는 클래스 수준에서 @RequestMapping 애노테이션을 가져야 합니다. */
  @Test
  public void 요청_매핑_검증() {
    ArchRule rule =
        classes()
            .that()
            .areAnnotatedWith(RestController.class)
            .or()
            .areAnnotatedWith(Controller.class)
            .should()
            .beAnnotatedWith(RequestMapping.class)
            .because("컨트롤러 클래스는 기본 경로 정의를 위해 클래스 수준의 @RequestMapping을 가져야 합니다");

    rule.check(importedClasses);
  }

  /**
   * HTTP 메서드 애노테이션 검증을 수행합니다. 컨트롤러의 public 메서드는 @GetMapping, @PostMapping 등의 HTTP 메서드 애노테이션을 가져야
   * 합니다.
   */
  @Test
  public void HTTP_메서드_애노테이션_검증() {
    ArchRule rule =
        methods()
            .that()
            .areDeclaredInClassesThat()
            .areAnnotatedWith(RestController.class)
            .or()
            .areDeclaredInClassesThat()
            .areAnnotatedWith(Controller.class)
            .and()
            .arePublic()
            .and()
            .areNotStatic()
            .should()
            .beAnnotatedWith(GetMapping.class)
            .orShould()
            .beAnnotatedWith(PostMapping.class)
            .orShould()
            .beAnnotatedWith(PutMapping.class)
            .orShould()
            .beAnnotatedWith(DeleteMapping.class)
            .orShould()
            .beAnnotatedWith(PatchMapping.class)
            .orShould()
            .beAnnotatedWith(RequestMapping.class)
            .because("컨트롤러의 public 메서드는 HTTP 메서드 애노테이션(@GetMapping, @PostMapping 등)을 가져야 합니다");

    rule.check(importedClasses);
  }

  /** 계층 의존성 방향을 검증합니다. 컨트롤러는 서비스에 의존할 수 있지만, 리포지토리에 직접 접근해서는 안 됩니다. */
  @Test
  public void 컨트롤러_계층_의존성_검증() {
    ArchRule rule =
        noClasses()
            .that()
            .areAnnotatedWith(RestController.class)
            .or()
            .areAnnotatedWith(Controller.class)
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..repository..")
            .because("컨트롤러는 리포지토리에 직접 접근하지 않고 서비스 계층을 통해 접근해야 합니다");

    rule.check(importedClasses);
  }

  /** 도메인 모델(엔티티) 노출 방지를 검증합니다. 컨트롤러 메서드는 도메인 엔티티를 직접 반환하지 않고 DTO나 ResponseEntity를 사용해야 합니다. */
  @Test
  public void 도메인_모델_노출_방지_검증() {
    // 도메인 엔티티 클래스 정의 (JPA @Entity 애노테이션이 있는 클래스)
    DescribedPredicate<JavaClass> isEntityClass =
        new DescribedPredicate<>("JPA Entity 클래스") {
          @Override
          public boolean test(JavaClass javaClass) {
            return javaClass.isAnnotatedWith("jakarta.persistence.Entity");
          }
        };

    // 컨트롤러 메서드가 엔티티를 직접 반환하지 않아야 함
    ArchRule rule =
        methods()
            .that()
            .areDeclaredInClassesThat()
            .areAnnotatedWith(RestController.class)
            .or()
            .areDeclaredInClassesThat()
            .areAnnotatedWith(Controller.class)
            .should(notReturnDomainEntities(isEntityClass))
            .because("컨트롤러 메서드는 도메인 엔티티를 직접 반환하지 않고 DTO나 ResponseEntity를 사용해야 합니다");

    rule.check(importedClasses);
  }

  /** 응답 래핑 일관성을 검증합니다. 컨트롤러 메서드는 일관된 응답 형식(ResponseEntity)을 사용해야 합니다. */
  @Test
  public void 응답_래핑_일관성_검증() {
    ArchRule rule =
        methods()
            .that()
            .areDeclaredInClassesThat()
            .areAnnotatedWith(RestController.class)
            .and()
            .arePublic()
            .and()
            .doNotHaveRawReturnType(void.class)
            .should()
            .haveRawReturnType(ResponseEntity.class)
            .because("REST 컨트롤러의 메서드는 일관된 응답 구조를 위해 ResponseEntity를 반환해야 합니다");

    rule.check(importedClasses);
  }

  /** 요청 검증 적용을 확인합니다. @RequestBody가 붙은 파라미터에는 @Valid 애노테이션이 있어야 합니다. */
  @Test
  public void 요청_검증_적용_확인() {
    ArchRule rule =
        methods()
            .that()
            .areDeclaredInClassesThat()
            .areAnnotatedWith(RestController.class)
            .or()
            .areDeclaredInClassesThat()
            .areAnnotatedWith(Controller.class)
            .should(validateRequestBodies())
            .because("@RequestBody 파라미터는 @Valid 애노테이션으로 검증되어야 합니다");

    rule.check(importedClasses);
  }

  /** 컨트롤러에서 트랜잭션 사용을 금지합니다. 트랜잭션 관리는 서비스 계층의 책임이어야 합니다. */
  @Test
  public void 컨트롤러_트랜잭션_사용_금지() {
    ArchRule rule =
        noMethods()
            .that()
            .areDeclaredInClassesThat()
            .areAnnotatedWith(RestController.class)
            .or()
            .areDeclaredInClassesThat()
            .areAnnotatedWith(Controller.class)
            .should()
            .beAnnotatedWith(Transactional.class)
            .because("트랜잭션 관리는 컨트롤러가 아닌 서비스 계층의 책임입니다");

    rule.check(importedClasses);
  }

  /** 컨트롤러 메서드의 명명 규칙을 검증합니다. 메서드 이름은 HTTP 메서드와 리소스 동작을 명확하게 반영해야 합니다. */
  @Test
  public void 컨트롤러_메서드_명명_규칙_검증() {
    ArchRule rule =
        methods()
            .that()
            .areDeclaredInClassesThat()
            .areAnnotatedWith(RestController.class)
            .or()
            .areDeclaredInClassesThat()
            .areAnnotatedWith(Controller.class)
            .and()
            .arePublic()
            .should(followControllerMethodNamingConvention())
            .because("컨트롤러 메서드는 명확한 동사로 시작하는 명명 규칙을 따라야 합니다(예: getUser, createOrder)");

    rule.check(importedClasses);
  }

  /** 필드 주입 사용을 금지합니다. 컨트롤러에서는 생성자 주입 방식을 사용해야 합니다. */
  @Test
  public void 필드_주입_금지_검증() {
    ArchRule rule =
        noFields()
            .that()
            .areDeclaredInClassesThat()
            .areAnnotatedWith(RestController.class)
            .or()
            .areDeclaredInClassesThat()
            .areAnnotatedWith(Controller.class)
            .should()
            .beAnnotatedWith(Autowired.class)
            .because("필드 주입(@Autowired)은 금지되어 있으며, 생성자 주입을 사용해야 합니다");

    rule.check(importedClasses);
  }

  /** 컨트롤러 메서드가 도메인 엔티티를 직접 반환하지 않는지 확인하는 커스텀 조건입니다. */
  private ArchCondition<JavaMethod> notReturnDomainEntities(
      DescribedPredicate<JavaClass> isEntityClass) {
    return new ArchCondition<>("도메인 엔티티를 직접 반환하지 않아야 함") {
      @Override
      public void check(JavaMethod method, ConditionEvents events) {
        JavaClass returnType = method.getRawReturnType();

        // 리스트 또는 컬렉션 타입인 경우 제네릭 타입 파라미터를 확인
        if (returnType.getName().startsWith("java.util.List")
            || returnType.getName().startsWith("java.util.Collection")
            || returnType.getName().startsWith("java.util.Set")) {
          // 현재 ArchUnit의 한계로 제네릭 타입 파라미터를 직접 확인하기 어려움
          // 이상적으로는 여기서 제네릭 타입 파라미터가 엔티티인지 확인해야 함
          return;
        }

        // ResponseEntity인 경우 제네릭 타입 파라미터를 확인 (ArchUnit 한계로 직접 확인 어려움)
        if (returnType.getName().equals(ResponseEntity.class.getName())) {
          return;
        }

        // 반환 타입이 엔티티인 경우 위반
        if (isEntityClass.test(returnType)) {
          events.add(
              SimpleConditionEvent.violated(
                  method,
                  method.getFullName() + "는 도메인 엔티티 " + returnType.getSimpleName() + "를 직접 반환합니다"));
        } else {
          events.add(SimpleConditionEvent.satisfied(method, "도메인 엔티티를 직접 반환하지 않습니다"));
        }
      }
    };
  }

  /**
   * @RequestBody 파라미터에 @Valid 애노테이션이 있는지 확인하는 커스텀 조건입니다.
   */
  private ArchCondition<JavaMethod> validateRequestBodies() {
    return new ArchCondition<>("@RequestBody 파라미터에 @Valid 애노테이션 확인") {
      @Override
      public void check(JavaMethod method, ConditionEvents events) {
        boolean hasRequestBody =
            method.getParameters().stream()
                .anyMatch(param -> param.isAnnotatedWith(RequestBody.class));

        // @RequestBody가 없으면 검사 대상이 아님
        if (!hasRequestBody) {
          events.add(SimpleConditionEvent.satisfied(method, "이 메서드는 @RequestBody를 사용하지 않습니다"));
          return;
        }

        boolean allRequestBodiesValidated =
            method.getParameters().stream()
                .filter(param -> param.isAnnotatedWith(RequestBody.class))
                .allMatch(param -> param.isAnnotatedWith(Valid.class));

        if (!allRequestBodiesValidated) {
          events.add(
              SimpleConditionEvent.violated(
                  method, method.getFullName() + "는 @RequestBody 파라미터에 @Valid 애노테이션이 없습니다"));
        } else {
          events.add(
              SimpleConditionEvent.satisfied(method, "모든 @RequestBody 파라미터에 @Valid 애노테이션이 있습니다"));
        }
      }
    };
  }

  /** 컨트롤러 메서드가 명명 규칙을 따르는지 확인하는 커스텀 조건입니다. */
  private ArchCondition<JavaMethod> followControllerMethodNamingConvention() {
    return new ArchCondition<>("컨트롤러 메서드 명명 규칙을 따름") {
      @Override
      public void check(JavaMethod method, ConditionEvents events) {
        String methodName = method.getName();

        // 허용되는 동사 접두사와 의미를 Map으로 관리
        Map<String, String> allowedPrefixesMap = new HashMap<>();
        // 기존 동사 접두사
        allowedPrefixesMap.put("get", "조회하다");
        allowedPrefixesMap.put("find", "찾다");
        allowedPrefixesMap.put("retrieve", "검색하다");
        allowedPrefixesMap.put("create", "생성하다");
        allowedPrefixesMap.put("add", "추가하다");
        allowedPrefixesMap.put("update", "갱신하다");
        allowedPrefixesMap.put("modify", "수정하다");
        allowedPrefixesMap.put("delete", "삭제하다");
        allowedPrefixesMap.put("remove", "제거하다");
        allowedPrefixesMap.put("process", "처리하다");
        allowedPrefixesMap.put("handle", "다루다");
        allowedPrefixesMap.put("execute", "실행하다");
        allowedPrefixesMap.put("withdraw", "철회하다");
        allowedPrefixesMap.put("report", "보고하다");
        allowedPrefixesMap.put("verify", "검증하다");
        allowedPrefixesMap.put("register", "등록하다");
        allowedPrefixesMap.put("perform", "수행하다");
        allowedPrefixesMap.put("search", "검색하다");
        allowedPrefixesMap.put("fetch", "가져오다");
        allowedPrefixesMap.put("login", "로그인하다");
        allowedPrefixesMap.put("change", "변경하다");
        allowedPrefixesMap.put("restore", "복원하다");
        allowedPrefixesMap.put("reissue", "재발급하다");
        allowedPrefixesMap.put("signup", "가입하다");
        allowedPrefixesMap.put("check", "확인하다");

        boolean startsWithVerb =
            allowedPrefixesMap.keySet().stream().anyMatch(methodName::startsWith);

        // camelCase 확인
        boolean isCamelCase =
            Character.isLowerCase(methodName.charAt(0))
                && !methodName.contains("_")
                && methodName.matches("^[a-z][a-zA-Z0-9]*$");

        if (!isCamelCase) {
          events.add(
              SimpleConditionEvent.violated(
                  method, "메서드 이름 '" + methodName + "'은(는) 동사로 시작하는 camelCase 형태가 아닙니다"));
        } else if (!startsWithVerb) {
          events.add(
              SimpleConditionEvent.violated(
                  method, "메서드 이름 '" + methodName + "'은(는) 명명 규칙을 따르지 않습니다"));
        } else {
          events.add(SimpleConditionEvent.satisfied(method, "메서드 이름이 명명 규칙을 따릅니다"));
        }
      }
    };
  }
}
