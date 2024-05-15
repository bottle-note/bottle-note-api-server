package app.bottlenote.alcohols.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
public class Populars {
	private final Long alcoholId;
	private final String korName;
	private final String engName;
	private final Double rating;
	private final String korCategory;
	private final String engCategory;
	private final String imageUrl;
	private final Boolean isPicked;

	@Builder
	public Populars(Long alcoholId, String korName, String engName, Double rating, String korCategory, String engCategory, String imageUrl, Boolean isPicked) {
		this.alcoholId = alcoholId;
		this.korName = korName;
		this.engName = engName;
		this.rating = rating;
		this.korCategory = korCategory;
		this.engCategory = engCategory;
		this.imageUrl = imageUrl;
		this.isPicked = isPicked;
	}

	public static Populars of(Long alcoholId, String korName, String engName, Double rating, String korCategory, String engCategory, String imageUrl, Boolean isPicked) {
		return Populars.builder()
			.alcoholId(alcoholId)
			.korName(korName)
			.engName(engName)
			.rating(rating)
			.korCategory(korCategory)
			.engCategory(engCategory)
			.imageUrl(imageUrl)
			.isPicked(isPicked)
			.build();
	}
}
