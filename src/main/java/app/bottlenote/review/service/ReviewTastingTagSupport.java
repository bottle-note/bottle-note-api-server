package app.bottlenote.review.service;

import static app.bottlenote.review.exception.ReviewExceptionCode.INVALID_TASTING_TAG_LIST_SIZE;

import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewTastingTag;
import app.bottlenote.review.exception.ReviewException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@RequiredArgsConstructor
public class ReviewTastingTagSupport {

	private static final int TASTING_TAG_MAX_SIZE = 10;


	public void saveReviewTastingTag(List<String> tastingTags, Review review) {

		if (CollectionUtils.isEmpty(tastingTags)) {
			return;
		}
		Set<ReviewTastingTag> reviewTastingTags = tastingTags.stream()
			.map(tastingTag -> ReviewTastingTag.create(review, tastingTag))
			.collect(Collectors.toSet());

		if (!isValidReviewTastingTag(reviewTastingTags)) {
			throw new ReviewException(INVALID_TASTING_TAG_LIST_SIZE);
		}
		review.saveTastingTag(reviewTastingTags);
	}

	public void updateReviewTastingTags(List<String> tastingTags, Review review) {

		if (CollectionUtils.isEmpty(tastingTags)) {
			review.updateTastingTags(Collections.emptySet());
		}

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

		review.updateTastingTags(reviewTastingTags);
	}


	private boolean isValidReviewTastingTag(Set<ReviewTastingTag> reviewTastingTags) {
		return reviewTastingTags.size() <= TASTING_TAG_MAX_SIZE;
	}


}
