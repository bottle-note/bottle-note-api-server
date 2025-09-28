package app.bottlenote.support.business.dto.request;

import lombok.Builder;

public record BusinessSupportPageableRequest(Long cursor, Long pageSize) {
  @Builder
  public BusinessSupportPageableRequest {
    cursor = cursor != null ? cursor : 0L;
    pageSize = pageSize != null ? pageSize : 10L;
  }
}
