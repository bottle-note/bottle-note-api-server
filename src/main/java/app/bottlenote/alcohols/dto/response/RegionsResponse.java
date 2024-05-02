package app.bottlenote.alcohols.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(staticName = "of")
public class RegionsResponse {
	private final Long regionId;
	private final String korName;
	private final String engName;
	private final String description;
}
