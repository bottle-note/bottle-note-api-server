package app.bottlenote.review.domain;

import app.bottlenote.review.dto.vo.ReviewModifyVO;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Embeddable
public class ReviewLocation {

	@Comment("상호 명")
	@Column(name = "bar_name")
	private String barName;

	@Comment("도로명 주소")
	@Column(name = "street_address")
	private String streetAddress;

	@Comment("카테고리")
	@Column(name = "category")
	private String category;

	@Comment("지도 URL")
	@Column(name = "map_url")
	private String mapUrl;

	@Comment("위도(x좌표)")
	@Column(name = "latitude")
	private String latitude;

	@Comment("경도(y좌표)")
	@Column(name = "longitude")
	private String longitude;

	@Builder
	public ReviewLocation(String barName, String streetAddress, String category, String mapUrl, String latitude, String longitude) {
		this.barName = barName;
		this.streetAddress = streetAddress;
		this.category = category;
		this.mapUrl = mapUrl;
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public void modifyReviewLocation(ReviewModifyVO reviewModifyVO){
		this.barName = reviewModifyVO.getBarName();
		this.streetAddress = reviewModifyVO.getStreetAddress();
		this.category = reviewModifyVO.getCategory();
		this.mapUrl = reviewModifyVO.getMapUrl();
		this.latitude = reviewModifyVO.getLatitude();
		this.longitude = reviewModifyVO.getLongitude();
	}

}
