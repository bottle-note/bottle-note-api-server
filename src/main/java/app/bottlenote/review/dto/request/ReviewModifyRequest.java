package app.bottlenote.review.dto.request;

import app.bottlenote.review.domain.constant.ReviewDisplayStatus;
import app.bottlenote.review.domain.constant.SizeType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record ReviewModifyRequest(

	@NotEmpty(message = "CONTENT_NOT_EMPTY")
	@Size(max = 500)
	String content,

	@NotNull(message = "REVIEW_DISPLAY_STATUS_NOT_EMPTY")
	ReviewDisplayStatus status,

	@DecimalMin(value = "0.0", message = "PRICE_MINIMUM")
	@DecimalMax(value = "1000000000000", message = "PRICE_MAXIMUM")
	@JsonInclude()
	@JsonProperty(required = true)
	BigDecimal price,

	@JsonInclude()
	@JsonProperty(required = true)
	List<ReviewImageInfo> imageUrlList,

	@JsonInclude()
	@JsonProperty(required = true)
	SizeType sizeType,

	@JsonInclude()
	@JsonProperty(required = true)
	List<String> tastingTagList,

	@Valid
	@JsonInclude()
	@JsonProperty(required = true)
	LocationInfo locationInfo

) {
}
