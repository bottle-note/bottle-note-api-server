package app.bottlenote.rating.fixture;

import app.bottlenote.rating.dto.request.RatingListFetchRequest;
import app.bottlenote.rating.dto.response.RatingListFetchResponse;

import java.util.ArrayList;
import java.util.List;

public class RatingObjectFixture {

	public static RatingListFetchRequest ratingListFetchRequest(
		String keyword,
		String category,
		Long regionId
	) {
		return new RatingListFetchRequest(keyword, category, regionId, null, null, 0L, 10L);
	}

	public static RatingListFetchResponse.Info ratingListFetchResponseInfo(Long index) {
		return new RatingListFetchResponse.Info(
			index,
			"nonImage",
			"글렌 알라키",
			"Glen Araki",
			"싱글 몰트",
			"Single Malt",
			index % 2 == 0
		);
	}


	public static RatingListFetchResponse ratingListFetchResponse(
	) {
		long totalCount = 10;

		List<RatingListFetchResponse.Info> list = new ArrayList<>();

		for (long index = 0; index < totalCount; index++) {
			list.add(ratingListFetchResponseInfo(index));
		}

		return RatingListFetchResponse.create(totalCount, list);
	}

}
