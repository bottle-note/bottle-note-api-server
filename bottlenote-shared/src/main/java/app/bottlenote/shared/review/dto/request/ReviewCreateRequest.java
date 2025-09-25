package app.bottlenote.shared.review.dto.request;

import app.bottlenote.shared.review.constant.ReviewDisplayStatus;
import app.bottlenote.shared.review.constant.SizeType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record ReviewCreateRequest(
    @Min(value = 1, message = "REVIEW_ID_MINIMUM") @NotNull(message = "REVIEW_ID_REQUIRED")
        Long alcoholId,
    ReviewDisplayStatus status,
    @NotEmpty(message = "REVIEW_CONTENT_REQUIRED")
        @Size(max = 500, message = "REVIEW_CONTENT_MAXIMUM")
        String content,
    SizeType sizeType,
    @DecimalMin(value = "0.0", message = "PRICE_MINIMUM")
        @DecimalMax(value = "1000000000000", message = "PRICE_MAXIMUM")
        BigDecimal price,
    @Valid LocationInfoRequest locationInfo,
    @Valid List<ReviewImageInfoRequest> imageUrlList,
    List<String> tastingTagList,
    Double rating) {
  public ReviewCreateRequest {
    status = status == null ? ReviewDisplayStatus.PUBLIC : status;
    imageUrlList = imageUrlList == null ? List.of() : imageUrlList;
    rating = rating == null ? 0.0 : rating;
  }
}
