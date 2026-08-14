package app.bottlenote.history.dto.request;

import app.bottlenote.global.pagination.PaginationRequest;
import lombok.Builder;

public record ViewHistoryRequest(String cursor, Integer size) {

  public static final int DEFAULT_SIZE = 6;
  public static final int MAX_SIZE = 100;

  @Builder
  public ViewHistoryRequest {
    PaginationRequest page = PaginationRequest.of(cursor, size, DEFAULT_SIZE, MAX_SIZE);
    cursor = page.cursor();
    size = page.size();
  }
}
