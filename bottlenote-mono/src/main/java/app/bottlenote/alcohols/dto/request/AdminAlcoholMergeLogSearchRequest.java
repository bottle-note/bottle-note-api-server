package app.bottlenote.alcohols.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record AdminAlcoholMergeLogSearchRequest(
    Long targetAlcoholId,
    Long sourceAlcoholId,
    @Min(value = 0, message = "ALCOHOL_MERGE_LOG_PAGE_MINIMUM") Integer page,
    @Min(value = 1, message = "ALCOHOL_MERGE_LOG_SIZE_MINIMUM")
        @Max(value = 100, message = "ALCOHOL_MERGE_LOG_SIZE_MAXIMUM")
        Integer size) {

  public AdminAlcoholMergeLogSearchRequest {
    page = page != null ? page : 0;
    size = size != null ? size : 20;
  }
}
