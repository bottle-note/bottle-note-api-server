package app.bottlenote.campaigncontent.exception;

import app.bottlenote.global.exception.custom.code.ExceptionCode;
import org.springframework.http.HttpStatus;

public enum CampaignContentExceptionCode implements ExceptionCode {
  CAMPAIGN_CONTENT_NOT_FOUND(HttpStatus.NOT_FOUND, "캠페인 콘텐츠를 찾을 수 없습니다."),
  CAMPAIGN_CONTENT_INVALID_CODE(
      HttpStatus.BAD_REQUEST, "코드는 영문 소문자·숫자와 단어 사이의 하이픈만 쓸 수 있고 50자 이하여야 합니다."),
  CAMPAIGN_CONTENT_DUPLICATE_CODE(HttpStatus.CONFLICT, "같은 코드의 캠페인 콘텐츠가 이미 있습니다."),
  CAMPAIGN_CONTENT_HAS_EVENTS(HttpStatus.CONFLICT, "참여 기록이 있는 캠페인 콘텐츠는 삭제할 수 없습니다. 비활성화해 주세요."),
  CAMPAIGN_CONTENT_INVALID_METRICS_RANGE(
      HttpStatus.BAD_REQUEST, "지표 조회 기간은 최근 90일 이내이고 시작일이 종료일보다 늦을 수 없습니다.");

  private final HttpStatus httpStatus;
  private final String message;

  CampaignContentExceptionCode(HttpStatus httpStatus, String message) {
    this.httpStatus = httpStatus;
    this.message = message;
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public HttpStatus getHttpStatus() {
    return httpStatus;
  }
}
