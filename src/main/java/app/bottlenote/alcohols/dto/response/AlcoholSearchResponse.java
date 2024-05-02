package app.bottlenote.alcohols.dto.response;

import app.bottlenote.rating.domain.RatingPoint;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor(staticName = "of")
public class AlcoholSearchResponse {
	private final Long totalCount;
	private final List<SearchDetail> alcohols;

	public record SearchDetail(String imageUrl,
							   Long alcoholId,
							   String korName,
							   String engName,
							   String categoryName,
							   RatingPoint rating,
							   Long count,
							   Boolean liked) {
	}
}
