package app.bottlenote.review.dto.vo;

import app.bottlenote.review.domain.constant.ReviewDisplayStatus;
import app.bottlenote.review.domain.constant.SizeType;
import app.bottlenote.review.dto.request.ReviewModifyRequest;
import java.math.BigDecimal;
import java.util.Objects;
import lombok.Getter;

@Getter
public class ReviewModifyVO {

	private final String content;

	private final ReviewDisplayStatus reviewDisplayStatus;

	private final BigDecimal price;

	private final SizeType sizeType;

	private final String zipCode;

	private final String address;

	private final String detailAddress;

	public ReviewModifyVO(ReviewModifyRequest reviewModifyRequest) {
		this.content = reviewModifyRequest.content();
		this.reviewDisplayStatus = reviewModifyRequest.status();
		this.price = reviewModifyRequest.price();
		this.sizeType = reviewModifyRequest.sizeType();
		if (Objects.isNull(reviewModifyRequest.locationInfo())) {
			this.zipCode = null;
			this.address = null;
			this.detailAddress = null;
		} else {
			this.zipCode = reviewModifyRequest.locationInfo().zipCode();
			this.address = reviewModifyRequest.locationInfo().address();
			this.detailAddress = reviewModifyRequest.locationInfo().detailAddress();
		}

	}
}
