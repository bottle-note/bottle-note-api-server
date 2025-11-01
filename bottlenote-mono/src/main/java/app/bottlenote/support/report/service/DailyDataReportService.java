package app.bottlenote.support.report.service;

import app.bottlenote.support.report.dto.DailyDataReportDto;
import app.bottlenote.support.report.exception.ReportException;
import app.bottlenote.support.report.exception.ReportExceptionCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyDataReportService {

  private final JdbcTemplate jdbcTemplate;

  @Qualifier("webhookRestTemplate")
  private final RestTemplate restTemplate;

  @Qualifier("webhookObjectMapper")
  private final ObjectMapper objectMapper;

  /**
   * 지정된 날짜의 데이터를 수집하고 웹훅으로 전송합니다.
   *
   * @param targetDate 리포트 대상 날짜
   * @param webhookUrl 웹훅 URL
   */
  public void collectAndSendDailyReport(LocalDate targetDate, String webhookUrl) {
    log.info("일일 데이터 리포트 수집 및 전송 시작: {}", targetDate);

    DailyDataReportDto report = collectDailyData(targetDate);
    sendWebhook(report, webhookUrl);

    log.info("일일 데이터 리포트 수집 및 전송 완료: {}", targetDate);
  }

  /**
   * 지정된 날짜의 데이터를 수집하여 리포트 DTO를 생성합니다.
   *
   * @param targetDate 리포트 대상 날짜
   * @return 일일 데이터 리포트 DTO
   */
  private DailyDataReportDto collectDailyData(LocalDate targetDate) {
    log.info("일일 데이터 리포트 수집 시작: {}", targetDate);

    try {
      LocalDateTime startOfDay = targetDate.atStartOfDay();
      LocalDateTime endOfDay = targetDate.plusDays(1).atStartOfDay();

      Long newUsersCount = countNewUsers(startOfDay, endOfDay);
      Long newReviewsCount = countNewReviews(startOfDay, endOfDay);
      Long newRepliesCount = countNewReplies(startOfDay, endOfDay);
      Long newLikesCount = countNewLikes(startOfDay, endOfDay);

      log.info(
          "일일 데이터 리포트 수집 완료: {} - 유저: {}, 리뷰: {}, 댓글: {}, 좋아요: {}",
          targetDate,
          newUsersCount,
          newReviewsCount,
          newRepliesCount,
          newLikesCount);

      return new DailyDataReportDto(
          targetDate, newUsersCount, newReviewsCount, newRepliesCount, newLikesCount);
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
  private void sendWebhook(DailyDataReportDto report, String webhookUrl) {
    if (webhookUrl == null || webhookUrl.isBlank()) {
      log.warn("웹훅 URL이 설정되지 않았습니다. 리포트 전송을 건너뜁니다.");
      return;
    }

    try {
      String message = buildDiscordMessage(report);
      Map<String, Object> payload = new HashMap<>();
      payload.put("content", message);

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      String jsonPayload = objectMapper.writeValueAsString(payload);
      HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);

      restTemplate.postForEntity(webhookUrl, request, String.class);
      log.info("Discord 웹훅 전송 완료: reportDate={}", report.reportDate());
    } catch (Exception e) {
      log.error(
          "Discord 웹훅 전송 실패: reportDate={}, error={}", report.reportDate(), e.getMessage(), e);
      throw new ReportException(ReportExceptionCode.WEBHOOK_SEND_FAILED);
    }
  }

  private String buildDiscordMessage(DailyDataReportDto report) {
    String dateStr = report.reportDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

    StringBuilder sb = new StringBuilder();
    sb.append("📊 **일일 데이터 리포트** - ").append(dateStr).append("\n\n");
    sb.append("👥 **신규 유저**: ").append(report.newUsersCount()).append("명\n");
    sb.append("✍️ **신규 리뷰**: ").append(report.newReviewsCount()).append("개\n");
    sb.append("💬 **신규 댓글**: ").append(report.newRepliesCount()).append("개\n");
    sb.append("❤️ **신규 좋아요**: ").append(report.newLikesCount()).append("개\n");

    return sb.toString();
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
}
