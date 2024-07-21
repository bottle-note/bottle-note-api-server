package app.bottlenote.alcohols.fixture;

import app.bottlenote.alcohols.dto.response.Populars;

import java.util.Random;

public class PopularsObjectFixture {

	public static Populars getFixturePopulars(
		Long alcoholId,
		String korName,
		String engName
	) {
		return Populars.builder()
			.alcoholId(alcoholId)
			.korName(korName)
			.engName(engName)
			.korCategory("싱글 몰트")
			.engCategory(engName)
			.rating(Math.round((Math.random() * 10) / 2) * 0.5)
			.ratingCount(new Random().nextLong(100) + 1)
			.korCategory("싱글 몰트")
			.engCategory("single molt")
			.imageUrl("https://i.imgur.com/TE2nmYV.png")
			.isPicked(alcoholId % 2 == 0)
			.build();
	}
}
