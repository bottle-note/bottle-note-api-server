package app.bottlenote.support.business.dto.request;

import app.bottlenote.support.business.constant.BusinessSupportType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Builder;

@Builder
public record BusinessSupportUpsertRequest(
    @NotEmpty(message = "TITLE_NOT_EMPTY") @Size(max = 100) String title,
    @NotEmpty(message = "CONTENT_NOT_EMPTY") @Size(max = 500) String content,
    @NotEmpty(message = "CONTACT_NOT_EMPTY") String contact,
    @NotNull(message = "REQUIRED_CONTACT_TYPE") BusinessSupportType businessSupportType,
    @Valid List<BusinessImageItem> imageUrlList) {
  public BusinessSupportUpsertRequest {
    imageUrlList = imageUrlList == null ? List.of() : imageUrlList;
  }
}
