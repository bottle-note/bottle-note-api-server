package app.bottlenote.support.help.dto.response;

import app.bottlenote.support.constant.StatusType;

public record AdminHelpAnswerResponse(Long helpId, StatusType status, String message) {

  public static AdminHelpAnswerResponse of(Long helpId, StatusType status) {
    return new AdminHelpAnswerResponse(helpId, status, "답변이 등록되었습니다.");
  }
}
