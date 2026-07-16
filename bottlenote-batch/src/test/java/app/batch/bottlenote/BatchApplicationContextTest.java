package app.batch.bottlenote;

import static org.assertj.core.api.Assertions.assertThat;

import app.batch.bottlenote.job.ranking.BestReviewSelectionJobConfig.BestReviewQuartzJob;
import app.batch.bottlenote.job.ranking.PopularAlcoholSelectionJobConfig.PopularAlcoholQuartzJob;
import app.batch.bottlenote.job.report.DailyDataReportJobConfig.DailyDataReportQuartzJob;
import app.bottlenote.review.service.ReviewService;
import app.bottlenote.support.report.service.DailyDataReportService;
import app.bottlenote.support.report.service.ReviewReportService;
import app.bottlenote.support.report.service.UserReportService;
import app.bottlenote.user.service.AdminUserService;
import app.bottlenote.user.service.OauthService;
import app.external.version.config.AppInfoConfig;
import app.external.webhook.config.DiscordWebhookProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Tag("batch")
@Testcontainers
@SpringBootTest(classes = BatchApplication.class)
@DisplayName("[batch] BatchApplication context")
class BatchApplicationContextTest {

  @Container
  static final MySQLContainer<?> MYSQL =
      new MySQLContainer<>(DockerImageName.parse("mysql:8.0.32"))
          .withDatabaseName("bottlenote_batch")
          .withUsername("root")
          .withPassword("root");

  @Autowired private ApplicationContext context;

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
    registry.add("spring.datasource.username", MYSQL::getUsername);
    registry.add("spring.datasource.password", MYSQL::getPassword);
    registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    registry.add("spring.quartz.jdbc.initialize-schema", () -> "always");
    registry.add("spring.batch.jdbc.initialize-schema", () -> "always");
    registry.add("webhook.discord.url", () -> "https://discord.test.webhook.url");
  }

  @Test
  @DisplayName("배치에 필요한 Job과 Quartz binding을 로드한다")
  void contextLoadsBatchJobsAndQuartzBindings() {
    assertThat(context.getBean("bestReviewSelectedJob", Job.class)).isNotNull();
    assertThat(context.getBean("popularAlcoholJob", Job.class)).isNotNull();
    assertThat(context.getBean("dailyDataReportJob", Job.class)).isNotNull();

    assertThat(context.getBean(BestReviewQuartzJob.class)).isNotNull();
    assertThat(context.getBean(PopularAlcoholQuartzJob.class)).isNotNull();
    assertThat(context.getBean(DailyDataReportQuartzJob.class)).isNotNull();
  }

  @Test
  @DisplayName("일일 리포트에 필요한 mono/external bean wiring을 유지한다")
  void contextKeepsDailyReportDependencies() {
    assertThat(context.getBean(DailyDataReportService.class)).isNotNull();
    assertThat(context.getBean("webhookRestTemplate", RestTemplate.class)).isNotNull();
    assertThat(context.getBean(DiscordWebhookProperties.class).getUrl())
        .isEqualTo("https://discord.test.webhook.url");
    assertThat(context.getBean(AppInfoConfig.class).getEnvironment()).isEqualTo("test");
    assertThat(context.getBean(StringRedisTemplate.class)).isNotNull();
  }

  @Test
  @DisplayName("batch와 무관한 product/admin 성격의 mono bean은 로드하지 않는다")
  void contextDoesNotLoadUnnecessaryMonoBeans() {
    assertThat(context.getBeansOfType(OauthService.class)).isEmpty();
    assertThat(context.getBeansOfType(AdminUserService.class)).isEmpty();
    assertThat(context.getBeansOfType(ReviewService.class)).isEmpty();
    assertThat(context.getBeansOfType(UserReportService.class)).isEmpty();
    assertThat(context.getBeansOfType(ReviewReportService.class)).isEmpty();
    assertThat(context.containsBean("visitorTelemetryStreamConsumer")).isFalse();
  }
}
