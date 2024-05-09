package app.bottlenote.review.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(staticName = "of")
public class ReviewResponse {

	private final Long totalCount;
	private final List<ReviewDetail> reviewList;

}
