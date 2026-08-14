package app.bottlenote.user.dto.request;

import app.bottlenote.global.pagination.PaginationRequest;
import lombok.Builder;

public record FollowPageableRequest(String cursor, Integer size) {
  public static final int DEFAULT_SIZE = 50;
  public static final int MAX_SIZE = 100;

  @Builder
  public FollowPageableRequest {
    PaginationRequest page = PaginationRequest.of(cursor, size, DEFAULT_SIZE, MAX_SIZE);
    cursor = page.cursor();
    size = page.size();
  }
}
