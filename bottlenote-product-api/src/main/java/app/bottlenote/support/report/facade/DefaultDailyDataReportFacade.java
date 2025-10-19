package app.bottlenote.support.report.facade;

import app.bottlenote.common.annotation.FacadeService;
import app.bottlenote.support.report.dto.DailyDataReportDto;
import app.bottlenote.support.report.service.DailyDataReportService;
import app.external.webhook.service.DiscordWebhookService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@FacadeService
@RequiredArgsConstructor
public class DefaultDailyDataReportFacade implements DailyDataReportFacade {

  private final DailyDataReportService dailyDataReportService;
  private final DiscordWebhookService discordWebhookService;

  @Override
  public void sendDailyReportWebhook(LocalDate targetDate) {
    log.info("일일 리포트 웹훅 전송 시작: {}", targetDate);

    try {
      DailyDataReportDto report = dailyDataReportService.getReportForWebhook(targetDate);

      if (report.webhookSent() != null && report.webhookSent()) {
        log.info("이미 웹훅이 전송된 리포트입니다: {}", targetDate);
        return;
      }

      discordWebhookService.sendDailyReport(report);
      dailyDataReportService.markWebhookSent(targetDate);

      log.info("일일 리포트 웹훅 전송 완료: {}", targetDate);
    } catch (Exception e) {
      log.error("일일 리포트 웹훅 전송 실패: {}", targetDate, e);
      throw e;
    }
  }
}
