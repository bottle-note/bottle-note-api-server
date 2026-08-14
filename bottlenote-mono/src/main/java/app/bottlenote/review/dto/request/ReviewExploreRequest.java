package app.bottlenote.review.dto.request;

import app.bottlenote.global.pagination.PaginationRequest;
import java.util.List;
import lombok.Builder;

public record ReviewExploreRequest(List<String> keywords, String cursor, Integer size) {

  public static final int DEFAULT_SIZE = 20;
  public static final int MAX_SIZE = 100;

  @Builder
  public ReviewExploreRequest {
    keywords = keywords != null ? keywords : List.of();
    PaginationRequest page = PaginationRequest.of(cursor, size, DEFAULT_SIZE, MAX_SIZE);
    cursor = page.cursor();
    size = page.size();
  }
}
