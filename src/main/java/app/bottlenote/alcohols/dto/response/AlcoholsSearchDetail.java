package app.bottlenote.alcohols.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
public class AlcoholsSearchDetail {
	private Long alcoholId;
	private String korName;
	private String engName;
	private String korCategoryName;
	private String engCategoryName;
	private String imageUrl;
	private Double rating;
	private Long ratingCount;
	private Long reviewCount;
	private Long pickCount;
	private Boolean picked;

	@Builder
	public AlcoholsSearchDetail(Long alcoholId, String korName, String engName, String korCategoryName, String engCategoryName, String imageUrl, Double rating, Long ratingCount, Long reviewCount, Long pickCount, Boolean picked) {
		this.alcoholId = alcoholId;
		this.korName = korName;
		this.engName = engName;
		this.korCategoryName = korCategoryName;
		this.engCategoryName = engCategoryName;
		this.imageUrl = imageUrl;
		this.rating = rating;
		this.ratingCount = ratingCount;
		this.reviewCount = reviewCount;
		this.pickCount = pickCount;
		this.picked = picked;
	}

	/**
	 * Dto Injection 을 위한 기본 생성자 사용 주의
	 */
	public AlcoholsSearchDetail() {
	}
}
