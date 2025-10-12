package app.bottlenote.support.report.facade;

import java.time.LocalDate;

public interface DailyDataReportFacade {

  void sendDailyReportWebhook(LocalDate targetDate);
}
