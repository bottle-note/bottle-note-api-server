package app.bottlenote.support.report.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReviewReportType {
  ADVERTISEMENT("광고"),
  PROFANITY("욕설");

  private final String description;
}
