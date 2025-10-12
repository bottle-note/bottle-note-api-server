package app.bottlenote.support.report.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.support.report.domain.DailyDataReport;
import app.bottlenote.support.report.dto.DailyDataReportDto;
import app.bottlenote.support.report.repository.DailyDataReportRepository;
import app.bottlenote.support.report.service.DailyDataReportService;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@Tag("integration")
@DisplayName("[integration] [service] DailyDataReportService")
class DailyDataReportIntegrationTest extends IntegrationTestSupport {

  @Autowired private DailyDataReportService dailyDataReportService;

  @Autowired private DailyDataReportRepository dailyDataReportRepository;

  @Test
  @DisplayName("전날 데이터를 수집하여 일일 리포트를 생성할 수 있다")
  @Sql(
      scripts = {
        "/init-script/init-daily-data-report.sql",
        "/init-script/init-user.sql",
        "/init-script/init-alcohol.sql",
        "/init-script/init-review.sql",
        "/init-script/init-review-reply.sql"
      })
  void test_1() {
    // given
    LocalDate targetDate = LocalDate.now().minusDays(1);

    // when
    DailyDataReport report = dailyDataReportService.collectDailyData(targetDate);

    // then
    assertNotNull(report);
    assertEquals(targetDate, report.getReportDate());
    assertNotNull(report.getNewUsersCount());
    assertNotNull(report.getNewReviewsCount());
    assertNotNull(report.getNewRepliesCount());
    assertNotNull(report.getNewLikesCount());
    assertEquals(false, report.getWebhookSent());

    log.info(
        "일일 리포트 생성 완료 - 유저: {}, 리뷰: {}, 댓글: {}, 좋아요: {}",
        report.getNewUsersCount(),
        report.getNewReviewsCount(),
        report.getNewRepliesCount(),
        report.getNewLikesCount());
  }

  @Test
  @DisplayName("리포트를 조회하고 웹훅 전송을 표시할 수 있다")
  @Sql(
      scripts = {
        "/init-script/init-daily-data-report.sql",
        "/init-script/init-user.sql",
        "/init-script/init-alcohol.sql",
        "/init-script/init-review.sql"
      })
  void test_2() {
    // given
    LocalDate targetDate = LocalDate.now().minusDays(1);
    DailyDataReport savedReport = dailyDataReportService.collectDailyData(targetDate);

    // when
    DailyDataReportDto reportDto = dailyDataReportService.getReportForWebhook(targetDate);

    // then
    assertNotNull(reportDto);
    assertEquals(targetDate, reportDto.reportDate());
    assertEquals(savedReport.getNewUsersCount(), reportDto.newUsersCount());
    assertEquals(savedReport.getNewReviewsCount(), reportDto.newReviewsCount());
    assertEquals(savedReport.getNewRepliesCount(), reportDto.newRepliesCount());
    assertEquals(savedReport.getNewLikesCount(), reportDto.newLikesCount());

    // when - 웹훅 전송 완료 표시
    dailyDataReportService.markWebhookSent(targetDate);

    // then
    DailyDataReport updatedReport =
        dailyDataReportRepository
            .findByReportDate(targetDate)
            .orElseThrow(() -> new IllegalArgumentException("리포트를 찾을 수 없습니다"));
    assertTrue(updatedReport.getWebhookSent());
    assertNotNull(updatedReport.getWebhookSentAt());
  }
}
