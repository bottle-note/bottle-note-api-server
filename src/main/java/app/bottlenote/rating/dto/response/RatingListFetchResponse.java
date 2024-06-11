package app.bottlenote.rating.dto.response;

import java.util.List;

public record RatingListFetchResponse(
	Long totalCount,
	List<Info> ratingList
) {
	public static RatingListFetchResponse create(Long totalCount, List<Info> ratingList) {
		return new RatingListFetchResponse(totalCount, ratingList);
	}

	public record Info(
		Long id,
		String imageUrl,
		String korName,
		String engName,
		String korCategoryName,
		String engCategoryName,
		Boolean isPicked
	) {
	}
}
