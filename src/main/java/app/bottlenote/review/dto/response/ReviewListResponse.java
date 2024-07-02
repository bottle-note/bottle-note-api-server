package app.bottlenote.review.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(staticName = "of")
public class ReviewListResponse {

	private final Long totalCount;
	private final List<ReviewResponse> reviewList;

}
