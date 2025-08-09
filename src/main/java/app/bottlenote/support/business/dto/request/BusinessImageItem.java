package app.bottlenote.support.business.dto.request;

import lombok.Builder;

@Builder
public record BusinessImageItem(Long order, String viewUrl) {
  public static BusinessImageItem create(Long order, String viewUrl) {
    return BusinessImageItem.builder().order(order).viewUrl(viewUrl).build();
  }
}
