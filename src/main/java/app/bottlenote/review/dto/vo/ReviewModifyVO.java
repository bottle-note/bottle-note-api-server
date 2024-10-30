package app.bottlenote.review.dto.vo;

import app.bottlenote.review.domain.constant.ReviewDisplayStatus;
import app.bottlenote.review.domain.constant.SizeType;
import app.bottlenote.review.dto.request.LocationInfo;
import app.bottlenote.review.dto.request.ReviewModifyRequest;
import java.math.BigDecimal;
import java.util.Objects;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class ReviewModifyVO {

	private final String content;

	private final ReviewDisplayStatus reviewDisplayStatus;

	private final BigDecimal price;

	private final SizeType sizeType;

	private final LocationInfo locationInfo;

	public ReviewModifyVO(ReviewModifyRequest reviewModifyRequest) {
		this.content = reviewModifyRequest.content();
		this.reviewDisplayStatus = reviewModifyRequest.status();
		this.price = reviewModifyRequest.price();
		this.sizeType = reviewModifyRequest.sizeType();
		if (Objects.isNull(reviewModifyRequest.locationInfo())) {
			this.locationInfo = new LocationInfo(null, null, null, null, null, null, null, null);
			return;
		}
		this.locationInfo = reviewModifyRequest.locationInfo();

	}
}
