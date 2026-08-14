package app.bottlenote.support.help.dto.request;

import app.bottlenote.global.pagination.PaginationRequest;
import app.bottlenote.support.constant.StatusType;
import app.bottlenote.support.help.constant.HelpType;
import lombok.Builder;

public record AdminHelpPageableRequest(
    StatusType status, HelpType type, String cursor, Integer size) {

  public static final int DEFAULT_SIZE = 20;
  public static final int MAX_SIZE = 100;

  @Builder
  public AdminHelpPageableRequest {
    PaginationRequest page = PaginationRequest.of(cursor, size, DEFAULT_SIZE, MAX_SIZE);
    cursor = page.cursor();
    size = page.size();
  }
}
