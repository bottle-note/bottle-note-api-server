package app.bottlenote.alcohols.domain.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

import java.util.Arrays;
import java.util.Set;

@Getter
public enum AlcoholCategoryGroup {
	SINGLE_MALT("싱글몰트 위스키", Set.of("Single Malts")),
	BLEND("블렌디드 위스키", Set.of("Blends")),
	BLENDED_MALT("블렌디드 몰트 위스키", Set.of("Blended Malts")),
	BOURBON("버번 위스키", Set.of("Bourbon")),
	RYE("라이 위스키", Set.of("Rye")),
	OTHER("기타 위스키", Set.of("Single Pot Still", "Single Grain", "Spirit", "Tennessee", "Wheat", "Corn"));

	private final String description;
	private final Set<String> categories;

	AlcoholCategoryGroup(String description, Set<String> categories) {
		this.description = description;
		this.categories = categories;
	}

	@JsonCreator
	public static AlcoholCategoryGroup fromCategory(String categoryGroup) {
		return Arrays.stream(values())
			.filter(group -> group.toString().equals(categoryGroup.toUpperCase()))
			.findFirst()
			.orElse(null);
	}
}
