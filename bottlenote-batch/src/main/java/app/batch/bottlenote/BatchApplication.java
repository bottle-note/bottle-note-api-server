package app.batch.bottlenote;

import app.bottlenote.support.report.service.DailyDataReportService;
import app.external.version.config.AppInfoConfig;
import app.external.webhook.config.WebhookConfig;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication(scanBasePackages = "app.batch.bottlenote")
@Import({DailyDataReportService.class, WebhookConfig.class, AppInfoConfig.class})
public class BatchApplication {
  public static void main(String[] args) {
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    SpringApplication.run(BatchApplication.class, args);
  }
}
