package app.bottlenote.review.dto.response;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode
@NoArgsConstructor
public class AlcoholInfo {

	private Long alcoholId;
	private String korName;
	private String engName;
	private String korCategoryName;
	private String engCategoryName;
	private String imageUrl;
	private Boolean isPicked;

	@Builder
	public AlcoholInfo(Long alcoholId, String korName, String engName, String korCategoryName, String engCategoryName, String imageUrl, Boolean isPicked) {
		this.alcoholId = alcoholId;
		this.korName = korName;
		this.engName = engName;
		this.korCategoryName = korCategoryName;
		this.engCategoryName = engCategoryName;
		this.imageUrl = imageUrl;
		this.isPicked = isPicked;
	}
}
