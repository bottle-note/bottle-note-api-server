package app.bottlenote.alcohols.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Populars {
	private final Long whiskyId;
	private final String korName;
	private final String engName;
	private final Double rating;
	private final String korCategory;
	private final String engCategory;
	private final String imageUrl;

	public static Populars of(Long whiskyId, String korName, String engName, Double rating, String korCategory, String engCategory, String imageUrl) {
		return new Populars(whiskyId, korName, engName, rating, korCategory, engCategory, imageUrl);
	}
}
