package app.bottlenote.support.block.dto.request;

import app.bottlenote.global.pagination.KeysetPageRequest;
import lombok.Builder;

public record BlockPageableRequest(String cursor, Integer size) {

  public static final int DEFAULT_SIZE = 10;
  public static final int MAX_SIZE = 100;

  @Builder
  public BlockPageableRequest {
    KeysetPageRequest page = KeysetPageRequest.of(cursor, size, DEFAULT_SIZE, MAX_SIZE);
    cursor = page.cursor();
    size = page.size();
  }
}
