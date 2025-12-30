package app.bottlenote.support.help.dto.request;

import app.bottlenote.support.constant.StatusType;
import app.bottlenote.support.help.constant.HelpType;
import lombok.Builder;

public record AdminHelpPageableRequest(
    StatusType status, HelpType type, Long cursor, Long pageSize) {

  @Builder
  public AdminHelpPageableRequest {
    cursor = cursor != null ? cursor : 0L;
    pageSize = pageSize != null ? pageSize : 20L;
  }
}
