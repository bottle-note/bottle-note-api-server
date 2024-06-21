package app.bottlenote.review.dto.request;

import app.bottlenote.review.domain.constant.ReviewStatus;
import app.bottlenote.review.domain.constant.SizeType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;


public record ReviewModifyRequest(
	@NotEmpty(message = "리뷰 내용을 입력해주세요")
	@Size(max = 500)
	String content,

	ReviewStatus status,

	@DecimalMin(value = "0.0", message = "가격은 0 이상이어야 합니다.")
	@DecimalMax(value = "1000000000000", message = "입력할 수 있는 가격의 범위가 아닙니다.")
	BigDecimal price,

	SizeType sizeType,

	List<String> tastingTagList,

	@Valid
	LocationInfo locationInfo

) {
}
