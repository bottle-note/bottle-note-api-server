package app.bottlenote.alcohols.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

public record AdminDistilleryUpsertRequest(
    @NotBlank(message = "DISTILLERY_KOR_NAME_REQUIRED") String korName,
    @NotBlank(message = "DISTILLERY_ENG_NAME_REQUIRED") String engName,
    String imageUrl,
    @Min(value = 0, message = "DISTILLERY_SORT_ORDER_MINIMUM") Integer sortOrder) {

  @Builder
  public AdminDistilleryUpsertRequest {
    sortOrder = sortOrder != null ? sortOrder : 9999;
  }
}
