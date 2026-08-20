package app.bottlenote.curation.dto.request;

import app.bottlenote.global.pagination.PaginationRequest;
import app.bottlenote.global.service.cursor.SortOrder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Builder;

public record CurationFeedSearchRequest(
    @NotEmpty(message = "CURATION_CODE_REQUIRED")
        List<@NotBlank(message = "CURATION_CODE_REQUIRED") String> code,
    String keyword,
    String cursor,
    Integer size,
    CurationSortType sortType,
    SortOrder sortOrder) {

  public static final int DEFAULT_SIZE = 10;
  public static final int MAX_SIZE = 10;

  public CurationFeedSearchRequest(List<String> code, String keyword, String cursor, Integer size) {
    this(code, keyword, cursor, size, null, null);
  }

  @Builder
  public CurationFeedSearchRequest {
    code = code != null ? List.copyOf(code) : List.of();
    PaginationRequest page = PaginationRequest.of(cursor, size, DEFAULT_SIZE, MAX_SIZE);
    cursor = page.cursor();
    size = page.size();
    sortType = sortType != null ? sortType : CurationSortType.EXPOSURE_START_DATE;
    sortOrder = sortOrder != null ? sortOrder : SortOrder.DESC;
  }
}
