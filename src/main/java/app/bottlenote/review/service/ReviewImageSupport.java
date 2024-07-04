package app.bottlenote.review.service;

import static app.bottlenote.review.exception.ReviewExceptionCode.INVALID_IMAGE_URL_MAX_SIZE;

import app.bottlenote.common.image.ImageUtil;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewImage;
import app.bottlenote.review.dto.request.ReviewImageInfo;
import app.bottlenote.review.exception.ReviewException;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewImageSupport {

	private static final int REVIEW_IMAGE_MAX_SIZE = 5;

	public List<ReviewImageInfo> getReviewImageInfo(Review review) {
		return review.getReviewImages().stream()
			.map(image -> ReviewImageInfo.create(image.getOrder(), image.getImageUrl()))
			.toList();
	}

	public void saveImages(List<ReviewImageInfo> imageList, Review review) {
		if (CollectionUtils.isEmpty(imageList)) {
			return;
		}
		List<ReviewImage> reviewImageList = imageList.stream()
			.map(image -> ReviewImage.builder()
				.order(image.order())
				.imageUrl(image.viewUrl())
				.imagePath(ImageUtil.getImagePath(image.viewUrl()))
				.imageKey(ImageUtil.getImageKey(image.viewUrl()))
				.imageName(ImageUtil.getImageName(image.viewUrl()))
				.review(review)
				.build()
			).toList();

		if (isOverMaxSize(reviewImageList)) {
			throw new ReviewException(INVALID_IMAGE_URL_MAX_SIZE);
		}
		review.saveImages(reviewImageList);
	}

	public void updateImages(List<ReviewImageInfo> imageList, Review review) {

		if (CollectionUtils.isEmpty(imageList)) {
			review.updateImages(Collections.emptyList());
		}
		List<ReviewImage> reviewImageList = imageList.stream()
			.map(image -> ReviewImage.builder()
				.order(image.order())
				.imageUrl(image.viewUrl())
				.imagePath(ImageUtil.getImagePath(image.viewUrl()))
				.imageKey(ImageUtil.getImageKey(image.viewUrl()))
				.imageName(ImageUtil.getImageName(image.viewUrl()))
				.review(review)
				.build()
			).toList();

		if (isOverMaxSize(reviewImageList)) {
			throw new ReviewException(INVALID_IMAGE_URL_MAX_SIZE);
		}

		review.updateImages(reviewImageList);

	}

	private boolean isOverMaxSize(List<ReviewImage> reviewImageList) {
		return reviewImageList.size() > REVIEW_IMAGE_MAX_SIZE;
	}

}
