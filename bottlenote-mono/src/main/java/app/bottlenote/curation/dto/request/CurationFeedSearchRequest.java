package app.bottlenote.curation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Builder;

public record CurationFeedSearchRequest(
    @NotEmpty(message = "CURATION_CODE_REQUIRED")
        List<@NotBlank(message = "CURATION_CODE_REQUIRED") String> code,
    String keyword,
    Long cursor,
    Integer size) {

  @Builder
  public CurationFeedSearchRequest {
    code = code != null ? List.copyOf(code) : List.of();
    cursor = cursor != null ? cursor : 0L;
    size = size != null ? size : 10;
  }
}
