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

	@NotEmpty(message = "REVIEW_NOT_EMPTY")
	@Size(max = 500)
	String content,

	@NotNull(message = "REVIEW_DISPLAY_STATUS_NOT_EMPTY")
	ReviewDisplayStatus status,

	@DecimalMin(value = "0.0", message = "가격은 0 이상이어야 합니다.")
	@DecimalMax(value = "1000000000000", message = "입력할 수 있는 가격의 범위가 아닙니다.")
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
