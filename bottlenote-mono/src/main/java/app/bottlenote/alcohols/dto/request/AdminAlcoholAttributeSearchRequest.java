package app.bottlenote.alcohols.dto.request;

import app.bottlenote.alcohols.constant.AttributeType;
import app.bottlenote.global.pagination.OffsetPageRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AdminAlcoholAttributeSearchRequest(
    @Size(max = 500, message = "ALCOHOL_ATTRIBUTE_BATCH_SIZE_INVALID")
        List<
                @NotNull(message = "ALCOHOL_ID_REQUIRED") @Positive(message = "ALCOHOL_ID_MINIMUM")
                Long>
            alcoholIds,
    String keyword,
    AttributeType type,
    Integer page,
    Integer size) {
  public AdminAlcoholAttributeSearchRequest {
    alcoholIds =
        alcoholIds == null
            ? List.of()
            : java.util.Collections.unmodifiableList(new java.util.ArrayList<>(alcoholIds));
    keyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
    var pagination = OffsetPageRequest.of(page, size, 20, 100);
    page = pagination.page();
    size = pagination.size();
  }
}
