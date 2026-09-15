package app.bottlenote.campaigncontent.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminCampaignContentCreateRequest(
    @NotBlank(message = "코드는 필수입니다.") @Size(max = 50, message = "코드는 50자 이하여야 합니다.") String code,
    @NotBlank(message = "이름은 필수입니다.") @Size(max = 100, message = "이름은 100자 이하여야 합니다.") String name,
    @Size(max = 255, message = "설명은 255자 이하여야 합니다.") String description,
    Boolean isActive) {

  public AdminCampaignContentCreateRequest {
    isActive = isActive != null ? isActive : true;
  }
}
