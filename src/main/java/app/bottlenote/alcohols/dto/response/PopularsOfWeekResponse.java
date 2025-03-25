package app.bottlenote.alcohols.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor(staticName = "of")
public class PopularsOfWeekResponse {
	private final Integer totalCount;
	private final List<PopularItem> alcohols;
}
