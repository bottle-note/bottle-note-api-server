package app.bottlenote.alcohols.dto.request;

import app.bottlenote.global.pagination.OffsetPageRequest;
import jakarta.validation.constraints.Positive;

public record AdminAlcoholAttributeMappingSearchRequest(
    @Positive(message = "ALCOHOL_ID_MINIMUM") Long alcoholId,
    @Positive(message = "ALCOHOL_ATTRIBUTE_ID_INVALID") Long attributeId,
    Integer page,
    Integer size) {
  public AdminAlcoholAttributeMappingSearchRequest {
    var pagination = OffsetPageRequest.of(page, size, 20, 100);
    page = pagination.page();
    size = pagination.size();
  }
}
