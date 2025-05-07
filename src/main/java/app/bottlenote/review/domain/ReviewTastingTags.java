package app.bottlenote.review.domain;

import app.bottlenote.review.exception.ReviewException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static app.bottlenote.review.exception.ReviewExceptionCode.INVALID_TASTING_TAG_LIST_SIZE;

@Getter
@Embeddable
@AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ReviewTastingTags {

	private static final int TASTING_TAG_MAX_SIZE = 10;

	@OneToMany(
			mappedBy = "review",
			fetch = FetchType.LAZY,
			cascade = CascadeType.ALL,
			orphanRemoval = true
	)
	private Set<ReviewTastingTag> reviewTastingTagSet = new HashSet<>();

	public static ReviewTastingTags empty() {
		return new ReviewTastingTags();
	}

	public void saveReviewTastingTag(List<String> reviewTastingTags, Review review) {
		if (CollectionUtils.isEmpty(reviewTastingTags)) {
			return;
		}
		Set<ReviewTastingTag> tastingTags = createTastingTagSet(reviewTastingTags, review);

		if (isInvalidReviewTastingTag(tastingTags)) {
			throw new ReviewException(INVALID_TASTING_TAG_LIST_SIZE);
		}
		this.reviewTastingTagSet.clear();
		this.reviewTastingTagSet.addAll(tastingTags);
	}

	public void updateReviewTastingTags(List<String> reviewTastingTags, Review review) {
		if (CollectionUtils.isEmpty(reviewTastingTags)) {
			this.reviewTastingTagSet.clear();
			return;
		}
		Set<ReviewTastingTag> tastingTags = createTastingTagSet(reviewTastingTags, review);

		if (isInvalidReviewTastingTag(tastingTags)) {
			throw new ReviewException(INVALID_TASTING_TAG_LIST_SIZE);
		}
		this.reviewTastingTagSet.clear();
		this.reviewTastingTagSet.addAll(tastingTags);
	}

	private Set<ReviewTastingTag> createTastingTagSet(List<String> reviewTastingTags, Review review) {
		return reviewTastingTags.stream()
				.map(String::trim)
				.filter(tag -> !tag.isEmpty())
				.distinct()
				.map(tastingTag -> ReviewTastingTag.create(review, tastingTag))
				.collect(Collectors.toSet());
	}

	private boolean isInvalidReviewTastingTag(Set<ReviewTastingTag> reviewTastingTags) {
		return reviewTastingTags.size() > TASTING_TAG_MAX_SIZE || reviewTastingTags.isEmpty();
	}
}
