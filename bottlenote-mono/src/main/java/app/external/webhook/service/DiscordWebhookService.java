package app.external.webhook.service;

import app.bottlenote.common.annotation.ThirdPartyService;
import app.bottlenote.support.report.dto.DailyDataReportDto;
import app.external.webhook.config.DiscordWebhookProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
@ThirdPartyService
public class DiscordWebhookService {

  private final DiscordWebhookProperties discordWebhookProperties;
  private final RestTemplate restTemplate = new RestTemplate();
  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Discord 웹훅으로 일일 리포트를 전송합니다.
   *
   * @param report 일일 데이터 리포트 DTO
   */
  public void sendDailyReport(DailyDataReportDto report) {
    try {
      String webhookUrl = discordWebhookProperties.getUrl();
      if (webhookUrl == null || webhookUrl.isBlank()) {
        log.warn("Discord 웹훅 URL이 설정되지 않았습니다. 리포트 전송을 건너뜁니다.");
        return;
      }

      String message = buildDiscordMessage(report);
      sendWebhook(webhookUrl, message);

      log.info("Discord 웹훅 전송 완료: reportDate={}", report.reportDate());
    } catch (Exception e) {
      log.error(
          "Discord 웹훅 전송 실패: reportDate={}, error={}", report.reportDate(), e.getMessage(), e);
      throw new RuntimeException("Discord 웹훅 전송 실패", e);
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

  private void sendWebhook(String webhookUrl, String message) {
    try {
      Map<String, Object> payload = new HashMap<>();
      payload.put("content", message);

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      String jsonPayload = objectMapper.writeValueAsString(payload);
      HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);

      restTemplate.postForEntity(webhookUrl, request, String.class);
    } catch (Exception e) {
      log.error("웹훅 전송 중 오류 발생: {}", e.getMessage(), e);
      throw new RuntimeException("웹훅 전송 실패", e);
    }
  }
}
