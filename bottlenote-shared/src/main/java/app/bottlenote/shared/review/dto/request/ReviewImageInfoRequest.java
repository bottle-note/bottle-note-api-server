package app.bottlenote.shared.review.dto.request;

import jakarta.validation.constraints.NotNull;

public record ReviewImageInfoRequest(
    @NotNull(message = "REVIEW_IMAGE_ORDER_REQUIRED") Long order,
    @NotNull(message = "REVIEW_IMAGE_URL_REQUIRED") String viewUrl) {

  public static ReviewImageInfoRequest create(Long order, String viewUrl) {
    return new ReviewImageInfoRequest(order, viewUrl);
  }
}
