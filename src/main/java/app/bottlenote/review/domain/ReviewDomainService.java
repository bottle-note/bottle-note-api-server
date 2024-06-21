package app.bottlenote.review.domain;

import static app.bottlenote.review.exception.ReviewExceptionCode.INVALID_TASTING_TAG_LIST_SIZE;

import app.bottlenote.review.dto.request.ReviewModifyRequest;
import app.bottlenote.review.exception.ReviewException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ReviewDomainService {

	private static final int TASTING_TAG_MAX_SIZE = 10;


	public ReviewModifyVO createValidatedReview(ReviewModifyRequest reviewModifyRequest) {
		return ReviewModifyVO.builder()
			.content(reviewModifyRequest.content())
			.reviewStatus(reviewModifyRequest.status())
			.price(reviewModifyRequest.price())
			.sizeType(reviewModifyRequest.sizeType())
			.zipCode(reviewModifyRequest.locationInfo().zipCode())
			.address(reviewModifyRequest.locationInfo().address())
			.detailAddress(reviewModifyRequest.locationInfo().detailAddress())
			.build();
	}

	public Set<ReviewTastingTag> createValidatedReviewTastingTag(List<String> tastingTags, Review review) {

		Set<ReviewTastingTag> reviewTastingTags = tastingTags.stream()
			.distinct() // 중복 제거
			.map(tastingTag -> ReviewTastingTag.builder()
				.review(review)
				.tastingTag(tastingTag)
				.build())
			.collect(Collectors.toSet());

		if (!isValidReviewTastingTag(reviewTastingTags)) {
			throw new ReviewException(INVALID_TASTING_TAG_LIST_SIZE);
		}

		return reviewTastingTags;
	}


	private boolean isValidReviewTastingTag(Set<ReviewTastingTag> reviewTastingTags) {
		return reviewTastingTags.size() <= TASTING_TAG_MAX_SIZE;
	}


}
