package app.bottlenote.review.dto.request;

import app.bottlenote.alcohols.domain.TastingTag;
import app.bottlenote.review.domain.constant.ReviewStatus;
import app.bottlenote.review.domain.constant.SizeType;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record ReviewCreateRequest(
	@NotNull(message = "alcohol id는 Null일 수 없습니다.")
	Long alcoholId,

	Double rating,

	ReviewStatus reviewStatus,

	String content,

	SizeType sizeType,

	BigDecimal price,

	LocationInfo locationInfo,

	String imageUrl,

	List<TastingTag> tastingTag

) {


}
