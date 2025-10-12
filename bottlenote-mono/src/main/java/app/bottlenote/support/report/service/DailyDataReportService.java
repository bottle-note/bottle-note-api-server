package app.bottlenote.support.report.service;

import app.bottlenote.support.report.domain.DailyDataReport;
import app.bottlenote.support.report.dto.DailyDataReportDto;
import app.bottlenote.support.report.repository.DailyDataReportRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyDataReportService {

  private final DailyDataReportRepository dailyDataReportRepository;
  private final JdbcTemplate jdbcTemplate;

  /**
   * 전날 데이터를 수집하여 일일 리포트를 생성합니다.
   *
   * @param targetDate 리포트 대상 날짜
   * @return 생성된 일일 리포트
   */
  @Transactional
  public DailyDataReport collectDailyData(LocalDate targetDate) {
    log.info("일일 데이터 리포트 수집 시작: {}", targetDate);

    LocalDateTime startOfDay = targetDate.atStartOfDay();
    LocalDateTime endOfDay = targetDate.plusDays(1).atStartOfDay();

    Long newUsersCount = countNewUsers(startOfDay, endOfDay);
    Long newReviewsCount = countNewReviews(startOfDay, endOfDay);
    Long newRepliesCount = countNewReplies(startOfDay, endOfDay);
    Long newLikesCount = countNewLikes(startOfDay, endOfDay);

    DailyDataReport report =
        DailyDataReport.builder()
            .reportDate(targetDate)
            .newUsersCount(newUsersCount)
            .newReviewsCount(newReviewsCount)
            .newRepliesCount(newRepliesCount)
            .newLikesCount(newLikesCount)
            .webhookSent(false)
            .build();

    DailyDataReport savedReport = dailyDataReportRepository.save(report);

    log.info(
        "일일 데이터 리포트 수집 완료: {} - 유저: {}, 리뷰: {}, 댓글: {}, 좋아요: {}",
        targetDate,
        newUsersCount,
        newReviewsCount,
        newRepliesCount,
        newLikesCount);

    return savedReport;
  }

  /**
   * 웹훅 전송 완료로 표시합니다.
   *
   * @param targetDate 대상 날짜
   */
  @Transactional
  public void markWebhookSent(LocalDate targetDate) {
    dailyDataReportRepository
        .findByReportDate(targetDate)
        .ifPresent(
            report -> {
              report.markWebhookSent();
              dailyDataReportRepository.save(report);
              log.info("웹훅 전송 완료 표시: reportDate={}", targetDate);
            });
  }

  /**
   * 지정된 날짜의 리포트를 조회하고 웹훅을 전송합니다.
   *
   * @param targetDate 대상 날짜
   */
  @Transactional
  public DailyDataReportDto getReportForWebhook(LocalDate targetDate) {
    DailyDataReport report =
        dailyDataReportRepository
            .findByReportDate(targetDate)
            .orElseThrow(() -> new IllegalArgumentException("리포트를 찾을 수 없습니다: " + targetDate));

    return new DailyDataReportDto(
        report.getReportDate(),
        report.getNewUsersCount(),
        report.getNewReviewsCount(),
        report.getNewRepliesCount(),
        report.getNewLikesCount(),
        report.getWebhookSent());
  }

  private Long countNewUsers(LocalDateTime start, LocalDateTime end) {
    String sql = "SELECT COUNT(*) FROM users WHERE create_at >= ? AND create_at < ?";
    return jdbcTemplate.queryForObject(sql, Long.class, start, end);
  }

  private Long countNewReviews(LocalDateTime start, LocalDateTime end) {
    String sql = "SELECT COUNT(*) FROM reviews WHERE create_at >= ? AND create_at < ?";
    return jdbcTemplate.queryForObject(sql, Long.class, start, end);
  }

  private Long countNewReplies(LocalDateTime start, LocalDateTime end) {
    String sql = "SELECT COUNT(*) FROM review_reply WHERE create_at >= ? AND create_at < ?";
    return jdbcTemplate.queryForObject(sql, Long.class, start, end);
  }

  private Long countNewLikes(LocalDateTime start, LocalDateTime end) {
    String sql = "SELECT COUNT(*) FROM likes WHERE create_at >= ? AND create_at < ?";
    return jdbcTemplate.queryForObject(sql, Long.class, start, end);
  }
}
