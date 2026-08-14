package app.bottlenote.support.business.dto.request;

import app.bottlenote.global.pagination.PaginationRequest;
import lombok.Builder;

public record BusinessSupportPageableRequest(String cursor, Integer size) {
  public static final int DEFAULT_SIZE = 10;
  public static final int MAX_SIZE = 100;

  @Builder
  public BusinessSupportPageableRequest {
    PaginationRequest page = PaginationRequest.of(cursor, size, DEFAULT_SIZE, MAX_SIZE);
    cursor = page.cursor();
    size = page.size();
  }
}
