package app.bottlenote.alcohols.dto.request;

import app.bottlenote.alcohols.constant.AttributeMatch;
import app.bottlenote.alcohols.constant.AttributeType;
import app.bottlenote.global.dto.request.OffsetPageRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record AdminAlcoholCompoundSearchRequest(
    String keyword,
    AttributeMatch match,
    @Size(max = 20, message = "ALCOHOL_ATTRIBUTE_BATCH_SIZE_INVALID")
        List<
                @NotNull(message = "ALCOHOL_ATTRIBUTE_INVALID_VALUE") @Valid
                AdminAlcoholAttributeCondition>
            conditions,
    AttributeType type,
    Boolean linkedOnly,
    Integer page,
    Integer size) {
  public AdminAlcoholCompoundSearchRequest {
    keyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
    match = match == null ? AttributeMatch.AND : match;
    conditions =
        conditions == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(conditions));
    linkedOnly = Boolean.TRUE.equals(linkedOnly);
    var pagination = OffsetPageRequest.of(page, size, 20, 100);
    page = pagination.page();
    size = pagination.size();
  }
}
