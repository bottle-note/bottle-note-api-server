package app.rule;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.core.importer.ClassFileImporter;

/**
 * 아키텍처 규칙 테스트의 공통 베이스.
 *
 * <p>임포트는 JVM당 한 번만 한다. `app.bottlenote` 아래 클래스 파일이 1,600개를 넘어 스캔이 비싼데, 규칙 검사는 임포트 결과를 변경하지 않으므로
 * 테스트마다 다시 읽을 이유가 없다. {@link JavaClasses}는 임포트 이후 불변이라 공유해도 안전하다.
 */
@SuppressWarnings({"JUnitTestClassNamingConvention"})
public abstract class AbstractRules {
  private static final JavaClasses IMPORTED_CLASSES =
      new ClassFileImporter().importPackages("app.bottlenote");

  static {
    // 임포트가 비면 모든 규칙이 공허하게 통과한다. 검사 대상이 실제로 있는지 확인한다.
    if (IMPORTED_CLASSES.size() == 0) {
      throw new IllegalStateException("app.bottlenote에서 검사 대상 클래스를 찾지 못했다");
    }
  }

  static {
    for (String name :
        new String[] {
          "app.bottlenote.accesscontrol.constant.IpBanActorType",
          "app.bottlenote.alcohols.constant.AdminAlcoholSortType",
          "app.bottlenote.statistics.config.StatisticsConfiguration",
          "app.bottlenote.global.config.WebConfig",
          "app.bottlenote.banner.constant.BannerAdminResultCode",
          "app.bottlenote.common.annotation.DomainEventListener",
          "app.bottlenote.global.redis.entity.AlcoholViewHistory",
          "app.bottlenote.notification.action.NotificationAction",
          "app.bottlenote.common.file.PreSignUrlProvider",
          "app.bottlenote.support.business.constant.BusinessResultMessage",
          "app.bottlenote.agent.constant.AgentStatus",
          "app.bottlenote.alcohols.domain.Alcohol",
          "app.bottlenote.common.domain.BaseEntity",
          "app.bottlenote.alcohols.repository.QueryDslAlcoholDetailQueryRepository"
        }) {
      if (!IMPORTED_CLASSES.contain(name)) {
        throw new IllegalStateException("검사 대상 모듈의 대표 클래스가 없다: " + name);
      }
    }
  }

  /**
   * admin-api에서 옮겨 온 코드가 있는 패키지. 이 패키지에는 이식한 코드만 있다.
   *
   * <p>admin-api는 product의 레이어 규칙을 적용받은 적이 없고, 공통 규칙 두 개(OpenAPI 파라미터 스키마, 응답 본문 타입)만 지켜 왔다. 이 두
   * 규칙은 product에도 같은 내용으로 있어 이식 코드까지 계속 검사한다. 이식 코드의 구조를 product 규칙에 맞추는 일은 동작을 바꾸지 않는 흡수 범위 밖이므로,
   * 이식 코드 때문에 실패하는 product 규칙만 {@link #productOwnedClasses}로 범위를 나눈다.
   */
  protected static final String[] ADMIN_API_PORTED_PACKAGES = {
    "app.bottlenote.accesscontrol.presentation..",
    "app.bottlenote.alcohols.presentation..",
    "app.bottlenote.alcohols.excel..",
    "app.bottlenote.auth.presentation..",
    "app.bottlenote.banner.presentation..",
    "app.bottlenote.campaigncontent.presentation..",
    "app.bottlenote.common.file.presentation..",
    "app.bottlenote.curation.presentation..",
    "app.bottlenote.mfds.presentation..",
    "app.bottlenote.review.presentation..",
    "app.bottlenote.statistics.presentation..",
    "app.bottlenote.support.help.presentation..",
    "app.bottlenote.user.presentation.."
  };

  /** 공유 패키지(alcohols.dto.response)에 놓인 이식 코드. 패키지로 나눌 수 없어 같은 파일의 클래스 이름으로 지정한다. */
  protected static final String ADMIN_API_PORTED_DTO =
      "app\\.bottlenote\\.alcohols\\.dto\\.response\\.AdminAlcoholExcel(ValidateResponse|RowResult|Issue)(\\$.*)?";

  protected final JavaClasses importedClasses = IMPORTED_CLASSES;

  /** product가 소유한 코드. 이식 코드 때문에 실패하는 product 규칙만 이 범위를 검사한다. */
  protected final JavaClasses productOwnedClasses =
      IMPORTED_CLASSES.that(
          DescribedPredicate.not(
              JavaClass.Predicates.resideInAnyPackage(ADMIN_API_PORTED_PACKAGES)
                  .or(HasName.Predicates.nameMatching(ADMIN_API_PORTED_DTO))));
}
