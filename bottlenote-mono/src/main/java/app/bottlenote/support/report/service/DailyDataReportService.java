package app.bottlenote.support.report.service;

import app.bottlenote.support.report.dto.response.DailyDataReportResponse;
import app.bottlenote.support.report.exception.ReportException;
import app.bottlenote.support.report.exception.ReportExceptionCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class DailyDataReportService {

  private final JdbcTemplate jdbcTemplate;
  private final RestTemplate restTemplate;

  public DailyDataReportService(
      JdbcTemplate jdbcTemplate, @Qualifier("webhookRestTemplate") RestTemplate restTemplate) {
    this.jdbcTemplate = jdbcTemplate;
    this.restTemplate = restTemplate;
  }

  /**
   * 지정된 날짜의 데이터를 수집하고 웹훅으로 전송합니다.
   *
   * @param targetDate 리포트 대상 날짜
   * @param webhookUrl 웹훅 URL
   */
  @Transactional(readOnly = true)
  public void collectAndSendDailyReport(LocalDate targetDate, String webhookUrl) {
    log.info("일일 데이터 리포트 수집 및 전송 시작: {}", targetDate);

    DailyDataReportResponse report = collectDailyData(targetDate);

    if (!report.hasNewData()) {
      log.info("신규 데이터가 없어 리포트 전송을 건너뜁니다: {}", targetDate);
      return;
    }

    sendWebhook(report, webhookUrl);

    log.info("일일 데이터 리포트 수집 및 전송 완료: {}", targetDate);
  }

  /**
   * 지정된 날짜의 데이터를 수집하여 리포트 DTO를 생성합니다.
   *
   * @param targetDate 리포트 대상 날짜
   * @return 일일 데이터 리포트 DTO
   */
  private DailyDataReportResponse collectDailyData(LocalDate targetDate) {
    log.info("일일 데이터 리포트 수집 시작: {}", targetDate);

    try {
      LocalDateTime startOfDay = targetDate.atStartOfDay();
      LocalDateTime endOfDay = targetDate.plusDays(1).atStartOfDay();

      Long newUsersCount = countNewUsers(startOfDay, endOfDay);
      Long newReviewsCount = countNewReviews(startOfDay, endOfDay);
      Long newRepliesCount = countNewReplies(startOfDay, endOfDay);
      Long newLikesCount = countNewLikes(startOfDay, endOfDay);
      Long newReportsCount = countUnprocessedReports();
      Long newInquiriesCount = countUnprocessedInquiries();

      log.info(
          "일일 데이터 리포트 수집 완료: {} - 유저: {}, 리뷰: {}, 댓글: {}, 좋아요: {}, 미처리 신고: {}, 미처리 문의: {}",
          targetDate,
          newUsersCount,
          newReviewsCount,
          newRepliesCount,
          newLikesCount,
          newReportsCount,
          newInquiriesCount);

      return new DailyDataReportResponse(
          targetDate,
          newUsersCount,
          newReviewsCount,
          newRepliesCount,
          newLikesCount,
          newReportsCount,
          newInquiriesCount);
    } catch (Exception e) {
      log.error("일일 데이터 수집 실패: targetDate={}, error={}", targetDate, e.getMessage(), e);
      throw new ReportException(ReportExceptionCode.DATA_COLLECTION_FAILED);
    }
  }

  /**
   * Discord 웹훅으로 리포트를 전송합니다.
   *
   * @param report 일일 데이터 리포트 DTO
   * @param webhookUrl 웹훅 URL
   */
  private void sendWebhook(DailyDataReportResponse report, String webhookUrl) {
    if (webhookUrl == null || webhookUrl.isBlank()) {
      log.warn("웹훅 URL이 설정되지 않았습니다. 리포트 전송을 건너뜁니다.");
      return;
    }

    try {
      String message = buildDiscordMessage(report);
      String jsonPayload = String.format("{\"content\":\"%s\"}", escapeJson(message));

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);

      restTemplate.postForEntity(webhookUrl, request, String.class);
      log.info("Discord 웹훅 전송 완료: reportDate={}", report.reportDate());
    } catch (Exception e) {
      log.error(
          "Discord 웹훅 전송 실패: reportDate={}, error={}", report.reportDate(), e.getMessage(), e);
      throw new ReportException(ReportExceptionCode.WEBHOOK_SEND_FAILED);
    }
  }

  private String escapeJson(String text) {
    return text.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }

  private String buildDiscordMessage(DailyDataReportResponse report) {
    return report.toDiscordMessage();
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
    String sql = "SELECT COUNT(*) FROM review_replies WHERE create_at >= ? AND create_at < ?";
    return jdbcTemplate.queryForObject(sql, Long.class, start, end);
  }

  private Long countNewLikes(LocalDateTime start, LocalDateTime end) {
    String sql = "SELECT COUNT(*) FROM likes WHERE create_at >= ? AND create_at < ?";
    return jdbcTemplate.queryForObject(sql, Long.class, start, end);
  }

  private Long countUnprocessedReports() {
    String reviewReportsSql =
        "SELECT COUNT(*) FROM review_reports WHERE status = 'WAITING' OR status = 'PENDING'";
    String userReportsSql =
        "SELECT COUNT(*) FROM user_reports WHERE status = 'WAITING' OR status = 'PENDING'";

    Long reviewReportsCount = jdbcTemplate.queryForObject(reviewReportsSql, Long.class);
    Long userReportsCount = jdbcTemplate.queryForObject(userReportsSql, Long.class);

    return (reviewReportsCount != null ? reviewReportsCount : 0L)
        + (userReportsCount != null ? userReportsCount : 0L);
  }

  private Long countUnprocessedInquiries() {
    String businessSupportsSql =
        "SELECT COUNT(*) FROM business_supports WHERE status = 'WAITING' OR status = 'PENDING'";
    Long count = jdbcTemplate.queryForObject(businessSupportsSql, Long.class);
    return count != null ? count : 0L;
  }
}
