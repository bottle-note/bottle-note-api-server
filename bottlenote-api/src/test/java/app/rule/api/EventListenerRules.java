package app.rule.api;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import app.bottlenote.common.annotation.DomainEventListener;
import app.rule.AbstractRules;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.event.TransactionalEventListener;

/** 이벤트 리스너 관련 아키텍처 규칙을 검증하는 테스트 클래스입니다. */
@Tag("rule")
@DisplayName("이벤트 리스너 아키텍처 규칙")
@SuppressWarnings({"NonAsciiCharacters", "JUnitTestClassNamingConvention"})
public class EventListenerRules extends AbstractRules {

  /** Spring Framework 라이프사이클 이벤트 제외 목록 (도메인 이벤트 규칙에서 제외) */
  private static final Set<Class<?>> EXCLUDED_SPRING_LIFECYCLE_EVENTS =
      Set.of(
          ApplicationReadyEvent.class,
          ApplicationStartedEvent.class,
          ContextRefreshedEvent.class,
          ContextStartedEvent.class,
          ContextStoppedEvent.class,
          ContextClosedEvent.class);

  private static boolean usesSpringLifecycleEvent(JavaMethod method) {
    // 1. 메서드 매개변수 확인
    boolean hasSpringLifecycleParam =
        method.getParameters().stream()
            .anyMatch(
                param ->
                    EXCLUDED_SPRING_LIFECYCLE_EVENTS.stream()
                        .anyMatch(
                            excludedClass ->
                                param.getType().getName().equals(excludedClass.getName())));

    if (hasSpringLifecycleParam) {
      return true;
    }

    // 2. @EventListener 어노테이션의 value/classes 속성 확인
    if (method.isAnnotatedWith(EventListener.class)) {
      String annotationString = method.getAnnotationOfType(EventListener.class).toString();
      if (EXCLUDED_SPRING_LIFECYCLE_EVENTS.stream()
          .anyMatch(excludedClass -> annotationString.contains(excludedClass.getName()))) {
        return true;
      }
    }

    // 3. @TransactionalEventListener 어노테이션의 value/classes 속성 확인
    if (method.isAnnotatedWith(TransactionalEventListener.class)) {
      String annotationString =
          method.getAnnotationOfType(TransactionalEventListener.class).toString();
      return EXCLUDED_SPRING_LIFECYCLE_EVENTS.stream()
          .anyMatch(excludedClass -> annotationString.contains(excludedClass.getName()));
    }

    return false;
  }

  @Test
  public void 이벤트_리스너_클래스_명명_규칙_검증() {
    ArchRule rule =
        classes()
            .that()
            .areMetaAnnotatedWith(DomainEventListener.class)
            .should()
            .haveSimpleNameEndingWith("Listener")
            .because("이벤트 리스너 클래스는 명확한 식별을 위해 'Listener'로 끝나야 합니다");
    rule.check(importedClasses);
  }

  @Test
  public void 이벤트_리스너_클래스는_DomainEventListener_어노테이션을_가져야_함() {
    ArchRule rule =
        classes()
            .that(
                new DescribedPredicate<>("eventListener 또는 TransactionAleVentListener 메소드가 있을 경우") {
                  @Override
                  public boolean test(JavaClass javaClass) {
                    return javaClass.getMethods().stream()
                        .anyMatch(
                            method ->
                                (method.isAnnotatedWith(EventListener.class)
                                        || method.isAnnotatedWith(TransactionalEventListener.class))
                                    && !usesSpringLifecycleEvent(method));
                  }
                })
            .should()
            .beAnnotatedWith(DomainEventListener.class)
            .because("이벤트 리스너 메서드가 있는 클래스는 반드시 @DomainEventListener 어노테이션을 가져야 합니다");

    rule.check(importedClasses);
  }

  @Test
  public void 이벤트_리스너_패키지_구조_검증() {
    ArchRule rule =
        classes()
            .that()
            .areMetaAnnotatedWith(DomainEventListener.class)
            .should()
            .resideInAPackage("..event.listener..")
            .because("이벤트 리스너 클래스는 구조적 일관성을 위해 '.event.listener' 패키지에 위치해야 합니다");
    rule.check(importedClasses);
  }

  @Test
  public void 이벤트_리스너_메서드_매개변수_검증() {
    ArchRule rule =
        methods()
            .that()
            .areAnnotatedWith(EventListener.class)
            .or()
            .areAnnotatedWith(TransactionalEventListener.class)
            .and(
                new DescribedPredicate<>("도메인 이벤트 리스너만 해당 (Spring 라이프사이클 이벤트 제외)") {
                  @Override
                  public boolean test(JavaMethod method) {
                    return !usesSpringLifecycleEvent(method);
                  }
                })
            .should(
                new ArchCondition<>("정확히 하나의 매개 변수가 있어야 한다") {
                  @Override
                  public void check(JavaMethod method, ConditionEvents events) {
                    boolean satisfied = method.getParameters().size() == 1;
                    String message =
                        String.format(
                            "이벤트 리스너 메서드 '%s'는 매개변수가 %d개로, 정확히 1개여야 한다는 규칙을 %s",
                            method.getFullName(),
                            method.getParameters().size(),
                            satisfied ? "준수합니다" : "위반합니다");

                    if (satisfied) {
                      events.add(SimpleConditionEvent.satisfied(method, message));
                    } else {
                      events.add(SimpleConditionEvent.violated(method, message));
                    }
                  }
                })
            .because("이벤트 리스너 메서드는 정확히 하나의 이벤트 객체를 매개변수로 받아야 합니다");

    rule.check(importedClasses);
  }

  @Test
  public void 이벤트_리스너_메서드_반환타입_검증() {
    ArchRule rule =
        methods()
            .that()
            .areAnnotatedWith(EventListener.class)
            .or()
            .areAnnotatedWith(TransactionalEventListener.class)
            .and(
                new DescribedPredicate<>("도메인 이벤트 리스너만 해당 (Spring 라이프사이클 이벤트 제외)") {
                  @Override
                  public boolean test(JavaMethod method) {
                    return !usesSpringLifecycleEvent(method);
                  }
                })
            .should()
            .haveRawReturnType(void.class)
            .because("이벤트 리스너 메서드는 void 반환 타입을 가져야 합니다");

    rule.check(importedClasses);
  }

  @Test
  public void 이벤트_리스너_메서드_명명_규칙_검증() {
    ArchRule rule =
        methods()
            .that()
            .areAnnotatedWith(EventListener.class)
            .or()
            .areAnnotatedWith(TransactionalEventListener.class)
            .and(
                new DescribedPredicate<>("도메인 이벤트 리스너만 해당 (Spring 라이프사이클 이벤트 제외)") {
                  @Override
                  public boolean test(JavaMethod method) {
                    return !usesSpringLifecycleEvent(method);
                  }
                })
            .should()
            .haveNameMatching("^(handle|process|on).*")
            .because("이벤트 리스너 메서드는 'handle', 'process', 'on' 등으로 시작하는 명확한 이름을 가져야 합니다");

    rule.check(importedClasses);
  }

  @Test
  public void 이벤트_리스너_매개변수_타입_검증() {
    ArchRule rule =
        methods()
            .that()
            .areAnnotatedWith(EventListener.class)
            .or()
            .areAnnotatedWith(TransactionalEventListener.class)
            .should(
                new ArchCondition<>(" '이벤트'로 끝나는 매개 변수 유형이 있어야한다.") {
                  @Override
                  public void check(JavaMethod method, ConditionEvents events) {
                    if (usesSpringLifecycleEvent(method)) {
                      events.add(
                          SimpleConditionEvent.satisfied(
                              method,
                              "Spring 라이프사이클 이벤트는 도메인 이벤트 규칙에서 제외됩니다: " + method.getFullName()));
                      return;
                    }

                    if (method.getParameters().isEmpty()) {
                      events.add(
                          SimpleConditionEvent.violated(
                              method, "이벤트 리스너 메서드 '" + method.getFullName() + "'는 매개변수가 없습니다"));
                      return;
                    }

                    String paramTypeName = method.getParameters().getFirst().getType().getName();
                    boolean satisfied = paramTypeName.endsWith("Event");

                    String message =
                        String.format(
                            "이벤트 리스너 메서드 '%s'의 매개변수 타입은 '%s'로, 'Event'로 끝나야 한다는 규칙을 %s",
                            method.getFullName(), paramTypeName, satisfied ? "준수합니다" : "위반합니다");

                    if (satisfied) {
                      events.add(SimpleConditionEvent.satisfied(method, message));
                    } else {
                      events.add(SimpleConditionEvent.violated(method, message));
                    }
                  }
                })
            .because("이벤트 리스너 메서드는 'Event'로 끝나는 이벤트 객체를 매개변수로 받아야 합니다");

    rule.check(importedClasses);
  }

  @Test
  public void 트랜잭션_이벤트_리스너_Phase_검증() {
    ArchRule rule =
        methods()
            .that()
            .areAnnotatedWith(TransactionalEventListener.class)
            .should(
                new ArchCondition<>("have explicitly defined phase") {
                  @Override
                  public void check(JavaMethod method, ConditionEvents events) {
                    if (usesSpringLifecycleEvent(method)) {
                      events.add(
                          SimpleConditionEvent.satisfied(
                              method,
                              "Spring 라이프사이클 이벤트는 도메인 이벤트 규칙에서 제외됩니다: " + method.getFullName()));
                      return;
                    }

                    // 어노테이션 선언 문자열 가져오기 (소스 코드에 작성된 형태)
                    String annotationString =
                        method.getAnnotationOfType(TransactionalEventListener.class).toString();

                    // phase= 문자열이 포함되어 있는지 확인
                    boolean hasExplicitPhase = annotationString.contains("phase=");

                    String message =
                        String.format(
                            "트랜잭션 이벤트 리스너 메서드 '%s'는 phase가 %s",
                            method.getFullName(),
                            hasExplicitPhase ? "명시적으로 설정되어 있습니다" : "명시적으로 설정되어 있지 않습니다");

                    if (hasExplicitPhase) {
                      events.add(SimpleConditionEvent.satisfied(method, message));
                    } else {
                      events.add(SimpleConditionEvent.violated(method, message));
                    }
                  }
                })
            .because("트랜잭션 이벤트 리스너는 명시적으로 phase를 설정해야 합니다 (AFTER_COMMIT, AFTER_ROLLBACK 등)");

    rule.check(importedClasses);
  }
}
