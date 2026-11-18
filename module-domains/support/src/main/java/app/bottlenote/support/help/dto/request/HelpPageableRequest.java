package app.bottlenote.support.help.dto.request;

import app.bottlenote.global.pagination.KeysetPageRequest;
import lombok.Builder;

public record HelpPageableRequest(String cursor, Integer size) {

  public static final int DEFAULT_SIZE = 10;
  public static final int MAX_SIZE = 100;

  @Builder
  public HelpPageableRequest {
    KeysetPageRequest page = KeysetPageRequest.of(cursor, size, DEFAULT_SIZE, MAX_SIZE);
    cursor = page.cursor();
    size = page.size();
  }

  public KeysetPageRequest page() {
    return new KeysetPageRequest(cursor, size);
  }
}
