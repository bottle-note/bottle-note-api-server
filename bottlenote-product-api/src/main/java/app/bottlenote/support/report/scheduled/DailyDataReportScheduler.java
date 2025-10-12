package app.bottlenote.support.report.scheduled;

import app.bottlenote.support.report.facade.DailyDataReportFacade;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyDataReportScheduler {

  private final DailyDataReportFacade dailyDataReportFacade;

  /** 매일 오전 10시 5분에 전날 리포트의 웹훅을 전송합니다. 데이터 수집 배치(10시)가 완료된 후 웹훅을 전송하기 위해 5분 지연시킵니다. */
  @Scheduled(cron = "0 5 10 * * ?")
  public void sendDailyReportWebhook() {
    try {
      LocalDate targetDate = LocalDate.now().minusDays(1);
      log.info("일일 리포트 웹훅 전송 스케줄러 시작: {}", targetDate);

      dailyDataReportFacade.sendDailyReportWebhook(targetDate);

      log.info("일일 리포트 웹훅 전송 스케줄러 완료: {}", targetDate);
    } catch (Exception e) {
      log.error("일일 리포트 웹훅 전송 스케줄러 실패", e);
    }
  }
}
