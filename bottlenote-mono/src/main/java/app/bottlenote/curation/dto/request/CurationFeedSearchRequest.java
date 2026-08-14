package app.bottlenote.curation.dto.request;

import app.bottlenote.global.pagination.PaginationRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Builder;

public record CurationFeedSearchRequest(
    @NotEmpty(message = "CURATION_CODE_REQUIRED")
        List<@NotBlank(message = "CURATION_CODE_REQUIRED") String> code,
    String keyword,
    String cursor,
    Integer size) {

  public static final int DEFAULT_SIZE = 10;
  public static final int MAX_SIZE = 10;

  @Builder
  public CurationFeedSearchRequest {
    code = code != null ? List.copyOf(code) : List.of();
    PaginationRequest page = PaginationRequest.of(cursor, size, DEFAULT_SIZE, MAX_SIZE);
    cursor = page.cursor();
    size = page.size();
  }
}
