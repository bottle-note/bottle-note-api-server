package app.bottlenote.accesscontrol.scheduled;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;

@Tag("unit")
@DisplayName("IP ban Quartz 작업 설정 단위 테스트")
class IpBanJobConfigurationTest {

  @Test
  @DisplayName("재조정 작업은 accessControl identity와 1분 cron으로 등록한다")
  void reconciliationJob_usesClusterSafeIdentityAndMinuteCron() {
    IpBanJobConfiguration configuration = new IpBanJobConfiguration();
    JobDetail detail = configuration.ipBanReconciliationJobDetail();
    CronTrigger trigger = (CronTrigger) configuration.ipBanReconciliationTrigger(detail);

    assertThat(detail.getKey().getGroup()).isEqualTo("accessControl");
    assertThat(detail.getKey().getName()).isEqualTo("ipBanReconciliationJob");
    assertThat(detail.requestsRecovery()).isTrue();
    assertThat(trigger.getCronExpression()).isEqualTo("0 */1 * * * ?");
  }

  @Test
  @DisplayName("보존 작업은 재조정 작업과 다른 identity를 사용한다")
  void retentionJob_usesSeparateIdentity() {
    IpBanJobConfiguration configuration = new IpBanJobConfiguration();
    JobDetail detail = configuration.ipBanRetentionJobDetail();

    assertThat(detail.getKey().getGroup()).isEqualTo("accessControl");
    assertThat(detail.getKey().getName()).isEqualTo("ipBanRetentionJob");
    assertThat(detail.requestsRecovery()).isTrue();
  }
}
