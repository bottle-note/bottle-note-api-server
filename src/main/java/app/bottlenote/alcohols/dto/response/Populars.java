package app.bottlenote.alcohols.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
public class Populars {
	private final String whiskyId;  // 1,
	private final String korName;  // '글렌피딕',
	private final String engName;  // 'glen fi',
	private final String rating;  // 3.5,
	private final String category;  // 'single molt',
	private final String imagePath;  // "https://i.imgur.com/TE2nmYV.png"

	@Builder
	public Populars(String whiskyId, String korName, String engName, String rating, String category, String imagePath) {
		this.whiskyId = whiskyId;
		this.korName = korName;
		this.engName = engName;
		this.rating = rating;
		this.category = category;
		this.imagePath = imagePath;
	}

	public static Populars of(String whiskyId, String korName, String engName, String rating, String category, String imagePath) {
		return Populars.builder()
			.whiskyId(whiskyId)
			.korName(korName)
			.engName(engName)
			.rating(rating)
			.category(category)
			.imagePath(imagePath)
			.build();
	}
}
