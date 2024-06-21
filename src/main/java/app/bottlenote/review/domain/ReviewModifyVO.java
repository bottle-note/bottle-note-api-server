package app.bottlenote.review.domain;

import app.bottlenote.review.domain.constant.ReviewStatus;
import app.bottlenote.review.domain.constant.SizeType;
import java.math.BigDecimal;
import lombok.Builder;
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

	@Builder
	public ReviewModifyVO(String content, ReviewStatus reviewStatus, BigDecimal price, SizeType sizeType, String zipCode, String address, String detailAddress) {
		this.content = content;
		this.reviewStatus = reviewStatus;
		this.price = price;
		this.sizeType = sizeType;
		this.zipCode = zipCode;
		this.address = address;
		this.detailAddress = detailAddress;
	}
}
