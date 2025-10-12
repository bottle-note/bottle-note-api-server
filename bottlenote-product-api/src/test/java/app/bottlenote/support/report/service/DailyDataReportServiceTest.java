package app.bottlenote.support.report.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bottlenote.support.report.domain.DailyDataReport;
import app.bottlenote.support.report.dto.DailyDataReportDto;
import app.bottlenote.support.report.repository.DailyDataReportRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@Tag("unit")
@DisplayName("[unit] [service] DailyDataReportService")
@ExtendWith(MockitoExtension.class)
class DailyDataReportServiceTest {

  @Mock private DailyDataReportRepository dailyDataReportRepository;

  @Mock private JdbcTemplate jdbcTemplate;

  @InjectMocks private DailyDataReportService dailyDataReportService;

  private LocalDate targetDate;

  @BeforeEach
  void setUp() {
    targetDate = LocalDate.of(2025, 10, 11);
  }

  @Test
  @DisplayName("일일 데이터 리포트를 수집할 수 있다")
  void test_1() {
    // given
    when(jdbcTemplate.queryForObject(
            anyString(), eq(Long.class), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(10L)
        .thenReturn(5L)
        .thenReturn(3L)
        .thenReturn(7L);

    DailyDataReport expectedReport =
        DailyDataReport.builder()
            .reportDate(targetDate)
            .newUsersCount(10L)
            .newReviewsCount(5L)
            .newRepliesCount(3L)
            .newLikesCount(7L)
            .webhookSent(false)
            .build();

    when(dailyDataReportRepository.save(any(DailyDataReport.class))).thenReturn(expectedReport);

    // when
    DailyDataReport result = dailyDataReportService.collectDailyData(targetDate);

    // then
    assertNotNull(result);
    assertEquals(targetDate, result.getReportDate());
    assertEquals(10L, result.getNewUsersCount());
    assertEquals(5L, result.getNewReviewsCount());
    assertEquals(3L, result.getNewRepliesCount());
    assertEquals(7L, result.getNewLikesCount());
    verify(jdbcTemplate, times(4))
        .queryForObject(
            anyString(), eq(Long.class), any(LocalDateTime.class), any(LocalDateTime.class));
    verify(dailyDataReportRepository, times(1)).save(any(DailyDataReport.class));
  }

  @Test
  @DisplayName("웹훅 전송을 위한 리포트를 조회할 수 있다")
  void test_2() {
    // given
    DailyDataReport report =
        DailyDataReport.builder()
            .reportDate(targetDate)
            .newUsersCount(10L)
            .newReviewsCount(5L)
            .newRepliesCount(3L)
            .newLikesCount(7L)
            .webhookSent(false)
            .build();

    when(dailyDataReportRepository.findByReportDate(targetDate)).thenReturn(Optional.of(report));

    // when
    DailyDataReportDto result = dailyDataReportService.getReportForWebhook(targetDate);

    // then
    assertNotNull(result);
    assertEquals(targetDate, result.reportDate());
    assertEquals(10L, result.newUsersCount());
    assertEquals(5L, result.newReviewsCount());
    assertEquals(3L, result.newRepliesCount());
    assertEquals(7L, result.newLikesCount());
    assertEquals(false, result.webhookSent());
    verify(dailyDataReportRepository, times(1)).findByReportDate(targetDate);
  }

  @Test
  @DisplayName("웹훅 전송 완료를 표시할 수 있다")
  void test_3() {
    // given
    DailyDataReport report =
        DailyDataReport.builder()
            .reportDate(targetDate)
            .newUsersCount(10L)
            .newReviewsCount(5L)
            .newRepliesCount(3L)
            .newLikesCount(7L)
            .webhookSent(false)
            .build();

    when(dailyDataReportRepository.findByReportDate(targetDate)).thenReturn(Optional.of(report));
    when(dailyDataReportRepository.save(any(DailyDataReport.class))).thenReturn(report);

    // when
    dailyDataReportService.markWebhookSent(targetDate);

    // then
    verify(dailyDataReportRepository, times(1)).findByReportDate(targetDate);
    verify(dailyDataReportRepository, times(1)).save(any(DailyDataReport.class));
  }
}
