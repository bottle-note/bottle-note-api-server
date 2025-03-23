package app.bottlenote.review.domain;

import app.bottlenote.review.dto.request.ReviewImageInfoRequest;
import app.bottlenote.review.exception.ReviewException;
import app.bottlenote.review.exception.ReviewExceptionCode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Embeddable
@AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ReviewImages {
	private static final int REVIEW_IMAGE_MAX_SIZE = 5;

	@Comment("리뷰 이미지 목록")
	@OneToMany(
		mappedBy = "review",
		fetch = FetchType.LAZY,
		cascade = CascadeType.ALL,
		orphanRemoval = true
	)
	private List<ReviewImage> images = new ArrayList<>();

	public static ReviewImages empty() {
		return new ReviewImages();
	}

	public void update(List<ReviewImage> list) {
		if (Objects.isNull(list) || list.isEmpty()) {
			this.images.clear();
			return;
		}

		if (isOverMaxSize(list))
			throw new ReviewException(ReviewExceptionCode.INVALID_IMAGE_URL_MAX_SIZE);

		this.images.clear();
		this.images.addAll(list);
	}

	private boolean isOverMaxSize(List<ReviewImage> reviewImageList) {
		return reviewImageList.size() > REVIEW_IMAGE_MAX_SIZE;
	}

	public List<ReviewImageInfoRequest> getViewInfo() {
		return this.images.stream()
			.map(
				image ->
					ReviewImageInfoRequest.create(
						image.getReviewImageInfo().getOrder(),
						image.getReviewImageInfo().getImageUrl()))
			.toList();
	}
}
