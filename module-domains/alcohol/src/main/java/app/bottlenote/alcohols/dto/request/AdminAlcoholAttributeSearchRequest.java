package app.bottlenote.alcohols.dto.request;

import app.bottlenote.alcohols.constant.AttributeType;
import app.bottlenote.global.dto.request.OffsetPageRequest;
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
    Integer size,
    @Size(max = 100, message = "ALCOHOL_ATTRIBUTE_GROUP_IDS_SIZE_INVALID")
        List<
                @NotNull(message = "ALCOHOL_ATTRIBUTE_GROUP_INVALID_VALUE")
                @Positive(message = "ALCOHOL_ATTRIBUTE_GROUP_INVALID_VALUE") Long>
            groupIds) {
  @org.springframework.beans.factory.annotation.Autowired
  public AdminAlcoholAttributeSearchRequest {
    groupIds =
        groupIds == null
            ? List.of()
            : java.util.Collections.unmodifiableList(new java.util.ArrayList<>(groupIds));
    alcoholIds =
        alcoholIds == null
            ? List.of()
            : java.util.Collections.unmodifiableList(new java.util.ArrayList<>(alcoholIds));
    keyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
    var pagination = OffsetPageRequest.of(page, size, 20, 100);
    page = pagination.page();
    size = pagination.size();
  }

  public AdminAlcoholAttributeSearchRequest(
      List<Long> alcoholIds, String keyword, AttributeType type, Integer page, Integer size) {
    this(alcoholIds, keyword, type, page, size, List.of());
  }
}
