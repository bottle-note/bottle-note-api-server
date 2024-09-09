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

	private final String barName;

	private final String streetAddress;

	private final String category;

	private final String mapUrl;

	private final String latitude;

	private final String longitude;

	public ReviewModifyVO(ReviewModifyRequest reviewModifyRequest) {
		this.content = reviewModifyRequest.content();
		this.reviewDisplayStatus = reviewModifyRequest.status();
		this.price = reviewModifyRequest.price();
		this.sizeType = reviewModifyRequest.sizeType();
		if (Objects.isNull(reviewModifyRequest.locationInfo())) {
			this.barName = null;
			this.streetAddress = null;
			this.category = null;
			this.mapUrl = null;
			this.latitude = null;
			this.longitude = null;
		} else {
			this.barName = reviewModifyRequest.locationInfo().barName();
			this.streetAddress = reviewModifyRequest.locationInfo().streetAddress();
			this.category = reviewModifyRequest.locationInfo().category();
			this.mapUrl = reviewModifyRequest.locationInfo().mapUrl();
			this.latitude = reviewModifyRequest.locationInfo().latitude();
			this.longitude = reviewModifyRequest.locationInfo().longitude();
		}

	}
}
