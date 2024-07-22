package app.bottlenote.review.dto.request;

import app.bottlenote.review.domain.constant.ReviewDisplayStatus;
import app.bottlenote.review.domain.constant.SizeType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record ReviewCreateRequest(
	@NotNull(message = "alcohol id는 Null일 수 없습니다.")
	Long alcoholId,

	ReviewDisplayStatus status,

	@NotEmpty(message = "리뷰 내용을 입력해주세요")
	@Size(max = 500)
	String content,

	SizeType sizeType,

	@DecimalMin(value = "0.0", message = "가격은 0 이상이어야 합니다.")
	@DecimalMax(value = "1000000000000", message = "입력할 수 있는 가격의 범위가 아닙니다.")
	BigDecimal price,

	@Valid
	LocationInfo locationInfo,

	List<ReviewImageInfo> imageUrlList,

	List<String> tastingTagList

) {
	public ReviewCreateRequest {
		status = status == null ? ReviewDisplayStatus.PUBLIC : status;
		imageUrlList = imageUrlList == null ? List.of() : imageUrlList;
	}
}
