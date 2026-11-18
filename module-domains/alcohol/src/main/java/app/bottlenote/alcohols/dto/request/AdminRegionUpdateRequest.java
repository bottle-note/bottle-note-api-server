package app.bottlenote.alcohols.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

public record AdminRegionUpdateRequest(
    @NotBlank(message = "REGION_KOR_NAME_REQUIRED") String korName,
    @NotBlank(message = "REGION_ENG_NAME_REQUIRED") String engName,
    String continent,
    String description,
    String imageUrl,
    Long parentId,
    @Min(value = 0, message = "REGION_SORT_ORDER_MINIMUM") Integer sortOrder) {

  @Builder
  public AdminRegionUpdateRequest {
    sortOrder = sortOrder != null ? sortOrder : 9999;
  }
}
