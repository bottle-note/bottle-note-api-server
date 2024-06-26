package app.bottlenote.review.domain;

import app.bottlenote.review.domain.constant.ReviewStatus;
import app.bottlenote.review.domain.constant.SizeType;
import app.bottlenote.review.dto.request.ReviewModifyRequest;
import java.math.BigDecimal;
import lombok.Getter;

@Getter
public class ReviewModifyVO {

	private final String content;

	private final ReviewStatus reviewStatus;

	private final BigDecimal price;

	private final SizeType sizeType;

	private final String zipCode;

	private final String address;

	private final String detailAddress;

	public ReviewModifyVO(ReviewModifyRequest reviewModifyRequest) {
		this.content = reviewModifyRequest.content();
		this.reviewStatus = reviewModifyRequest.status();
		this.price = reviewModifyRequest.price();
		this.sizeType = reviewModifyRequest.sizeType();
		this.zipCode = reviewModifyRequest.locationInfo().zipCode();
		this.address = reviewModifyRequest.locationInfo().address();
		this.detailAddress = reviewModifyRequest.locationInfo().detailAddress();
	}
}
