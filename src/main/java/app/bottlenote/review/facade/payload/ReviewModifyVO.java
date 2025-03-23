package app.bottlenote.review.facade.payload;

import app.bottlenote.review.constant.ReviewDisplayStatus;
import app.bottlenote.review.constant.SizeType;
import app.bottlenote.review.dto.request.LocationInfoRequest;
import app.bottlenote.review.dto.request.ReviewModifyRequest;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Objects;

@Getter
public class ReviewModifyVO {
	private final String content;
	private final ReviewDisplayStatus reviewDisplayStatus;
	private final BigDecimal price;
	private final SizeType sizeType;
	private final LocationInfoRequest locationInfo;

	public ReviewModifyVO(ReviewModifyRequest reviewModifyRequest) {
		this.content = reviewModifyRequest.content();
		this.reviewDisplayStatus = reviewModifyRequest.status();
		this.price = reviewModifyRequest.price();
		this.sizeType = reviewModifyRequest.sizeType();
		this.locationInfo = Objects.requireNonNullElse(reviewModifyRequest.locationInfo(), LocationInfoRequest.empty());
	}

	public static ReviewModifyVO create(ReviewModifyRequest reviewModifyRequest) {
		return new ReviewModifyVO(reviewModifyRequest);
	}
}
