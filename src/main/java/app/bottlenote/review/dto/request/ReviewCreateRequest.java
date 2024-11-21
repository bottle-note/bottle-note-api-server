package app.bottlenote.review.dto.request;

import app.bottlenote.review.domain.constant.ReviewDisplayStatus;
import app.bottlenote.review.domain.constant.SizeType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
public record ReviewCreateRequest(
	@Min(value = 1, message = "REVIEW_ID_MINIMUM")
	@NotNull(message = "REVIEW_ID_REQUIRED")
	Long alcoholId,

	ReviewDisplayStatus status,

	@NotEmpty(message = "REVIEW_CONTENT_REQUIRED")
	@Size(max = 500, message = "REVIEW_CONTENT_MAXIMUM")
	String content,

	SizeType sizeType,

	@DecimalMin(value = "0.0", message = "PRICE_MINIMUM")
	@DecimalMax(value = "1000000000000", message = "PRICE_MAXIMUM")
	BigDecimal price,

	@Valid
	LocationInfo locationInfo,

	@Valid
	List<ReviewImageInfo> imageUrlList,
	List<String> tastingTagList
) {
	public ReviewCreateRequest {
		status = status == null ? ReviewDisplayStatus.PUBLIC : status;
		imageUrlList = imageUrlList == null ? List.of() : imageUrlList;
	}
}
