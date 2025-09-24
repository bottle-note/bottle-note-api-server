package app.bottlenote.support.report.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserReportResponse {
  private String message;
  private Long reportId;
  private Long reportUserId;
  private String reportUserName;

  protected UserReportResponse(
      String message, Long reportId, Long reportUserId, String reportUserName) {
    this.message = message;
    this.reportId = reportId;
    this.reportUserId = reportUserId;
    this.reportUserName = reportUserName;
  }

  public static UserReportResponse of(
      UserReportResponseEnum message, Long reportId, Long reportUserId, String reportUserName) {
    return new UserReportResponse(message.getMessage(), reportId, reportUserId, reportUserName);
  }

  @AllArgsConstructor
  @Getter
  public enum UserReportResponseEnum {
    SUCCESS("신고가 성공적으로 접수되었습니다."),
    DUPLICATE("이미 신고한 사용자입니다."),
    SAME_USER("자신을 신고할 수 없습니다.");
    private final String message;
  }
}
