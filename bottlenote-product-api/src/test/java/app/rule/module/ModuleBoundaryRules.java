package app.rule.module;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("rule")
@DisplayName("실제 모듈 소유와 공개 조회 경계")
class ModuleBoundaryRules {
  private static final Map<String, Set<String>> DAG =
      Map.ofEntries(
          Map.entry("foundation", Set.of("observability")),
          Map.entry("user", Set.of("foundation")),
          Map.entry("alcohol", Set.of("foundation")),
          Map.entry("storage", Set.of("foundation", "user")),
          Map.entry(
              "community", Set.of("foundation", "user", "alcohol", "storage", "observability")),
          Map.entry(
              "history", Set.of("foundation", "user", "alcohol", "community", "observability")),
          Map.entry("support", Set.of("foundation", "user", "community")),
          Map.entry("notification", Set.of("foundation", "user", "community", "support")),
          Map.entry("analytics", Set.of("foundation")),
          Map.entry("content", Set.of("foundation", "user", "alcohol", "community", "analytics")),
          Map.entry("access-control", Set.of("foundation", "user")));
  private static final Map<String, String> REPRESENTATIVES =
      Map.ofEntries(
          Map.entry("access-control", "app.bottlenote.accesscontrol.constant.IpBanActorType"),
          Map.entry("alcohol", "app.bottlenote.alcohols.constant.AdminAlcoholSortType"),
          Map.entry("analytics", "app.bottlenote.statistics.config.StatisticsConfiguration"),
          Map.entry("community", "app.bottlenote.global.config.WebConfig"),
          Map.entry("content", "app.bottlenote.banner.constant.BannerAdminResultCode"),
          Map.entry("foundation", "app.bottlenote.common.annotation.DomainEventListener"),
          Map.entry("history", "app.bottlenote.global.redis.entity.AlcoholViewHistory"),
          Map.entry("notification", "app.bottlenote.notification.action.NotificationAction"),
          Map.entry("storage", "app.bottlenote.common.file.PreSignUrlProvider"),
          Map.entry("support", "app.bottlenote.support.business.constant.BusinessResultMessage"),
          Map.entry("user", "app.bottlenote.agent.constant.AgentStatus"));

  private final JavaClasses classes =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("app.bottlenote", "app.external");

  private static String owner(JavaClass type) {
    String location = type.getSource().map(source -> source.getUri().toString()).orElse("");
    for (String module : DAG.keySet()) {
      if (location.contains("/bottlenote-" + module + "/")
          || location.contains("/bottlenote-" + module + "-")) {
        return module;
      }
    }
    return "";
  }

  @Test
  @DisplayName("모든 업무 라이브러리와 외부 연동을 실제 importer에 포함한다")
  void importsEveryLibrary() {
    REPRESENTATIVES.forEach(
        (module, name) -> {
          assertThat(classes.contain(name)).as(name).isTrue();
          assertThat(owner(classes.get(name))).as(name).isEqualTo(module);
        });
    assertThat(classes.stream().anyMatch(type -> type.getPackageName().startsWith("app.external")))
        .isTrue();
  }

  @Test
  @DisplayName("라이브러리는 승인된 단방향 모듈만 참조한다")
  void followsModuleDag() {
    for (JavaClass type : classes) {
      String from = owner(type);
      if (from.isEmpty()) continue;
      type.getDirectDependenciesFromSelf()
          .forEach(
              dependency -> {
                String to = owner(dependency.getTargetClass());
                if (!to.isEmpty() && !from.equals(to)) {
                  assertThat(DAG.get(from)).as(dependency.getDescription()).contains(to);
                }
              });
      type.getDirectDependenciesFromSelf()
          .forEach(
              dependency -> {
                String location =
                    dependency
                        .getTargetClass()
                        .getSource()
                        .map(source -> source.getUri().toString())
                        .orElse("");
                assertThat(location)
                    .as(dependency.getDescription())
                    .doesNotContain(
                        "/bottlenote-product-api/", "/bottlenote-admin-api/", "/bottlenote-batch/");
              });
    }
  }

  @Test
  @DisplayName("업무 라이브러리는 다른 업무의 좁은 서비스 계약만 사용한다")
  void usesNarrowServiceContracts() {
    for (JavaClass type : classes) {
      String from = owner(type);
      if (from.isEmpty()) continue;
      type.getDirectDependenciesFromSelf()
          .forEach(
              dependency -> {
                JavaClass target = dependency.getTargetClass();
                String to = owner(target);
                if (to.isEmpty()
                    || from.equals(to)
                    || to.equals("foundation")
                    || !isServicePackage(target.getPackageName())) return;
                assertThat(target.getName())
                    .as(dependency.getDescription())
                    .isIn(
                        "app.bottlenote.common.file.service.ResourceVerifierService",
                        "app.bottlenote.common.file.service.ResourceCommandService",
                        "app.bottlenote.common.file.service.ImageUploadService");
              });
    }
  }

  @Test
  @DisplayName("service 패키지와 그 하위 패키지를 서비스 계약 검사에 포함한다")
  void recognizesServicePackages() {
    assertThat(isServicePackage("app.bottlenote.user.service")).isTrue();
    assertThat(isServicePackage("app.bottlenote.user.service.internal")).isTrue();
    assertThat(isServicePackage("app.bottlenote.user.facade")).isFalse();
    assertThat(isServicePackage("app.bottlenote.user.services")).isFalse();
  }

  private static boolean isServicePackage(String packageName) {
    return packageName.endsWith(".service") || packageName.contains(".service.");
  }

  @Test
  @DisplayName("타 모듈 조회 조인은 저장소의 쓰기 메서드를 호출하지 않는다")
  void joinsCannotWriteForeignRepositories() {
    for (JavaClass type : classes) {
      type.getMethodCallsFromSelf()
          .forEach(
              call -> {
                JavaClass target = call.getTarget().getOwner();
                String to = owner(target);
                if (to.isEmpty()
                    || to.equals(owner(type))
                    || !target.getPackageName().contains(".repository")) return;
                assertThat(call.getTarget().getName())
                    .as(call.getDescription())
                    .doesNotMatch("^(save|delete|remove|update|insert|flush|executeUpdate).*");
              });
    }
  }
}
