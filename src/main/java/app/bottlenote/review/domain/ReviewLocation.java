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
	@Column(name = "location_name")
	private String name;

	@Comment("우편 번호")
	@Column(name = "zip_code")
	private String zipCode;

	@Comment("도로명 주소")
	@Column(name = "address")
	private String address;

	@Comment("상세 주소")
	@Column(name = "detail_address")
	private String detailAddress;

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
	public ReviewLocation(String name, String zipCode, String address, String detailAddress, String category, String mapUrl, String latitude, String longitude) {
		this.name = name;
		this.zipCode = zipCode;
		this.address = address;
		this.detailAddress = detailAddress;
		this.category = category;
		this.mapUrl = mapUrl;
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public void modifyReviewLocation(ReviewModifyVO reviewModifyVO){
		this.name = reviewModifyVO.getLocationInfo().locationName();
		this.zipCode = reviewModifyVO.getLocationInfo().zipCode();
		this.address = reviewModifyVO.getLocationInfo().address();
		this.detailAddress = reviewModifyVO.getLocationInfo().detailAddress();
		this.category = reviewModifyVO.getLocationInfo().category();
		this.mapUrl = reviewModifyVO.getLocationInfo().mapUrl();
		this.latitude = reviewModifyVO.getLocationInfo().latitude();
		this.longitude = reviewModifyVO.getLocationInfo().longitude();
	}

}
