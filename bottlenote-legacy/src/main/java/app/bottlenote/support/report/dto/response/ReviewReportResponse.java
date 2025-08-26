package app.bottlenote.support.report.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

public record ReviewReportResponse(
    boolean success, ReviewReportResponseEnum message, String responseAt) {

  public static ReviewReportResponse response(boolean success) {
    return new ReviewReportResponse(
        success,
        success ? ReviewReportResponseEnum.SUCCESS : ReviewReportResponseEnum.FAIL,
        java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
  }

  @AllArgsConstructor
  @Getter
  public enum ReviewReportResponseEnum {
    SUCCESS("신고가 성공적으로 접수되었습니다."),
    FAIL("신고 접수에 실패했습니다.");

    private final String message;
  }
}
