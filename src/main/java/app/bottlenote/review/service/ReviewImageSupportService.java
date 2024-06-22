package app.bottlenote.review.service;

import static app.bottlenote.review.exception.ReviewExceptionCode.INVALID_IMAGE_URL_MAX_SIZE;

import app.bottlenote.common.image.ImageUtil;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewImage;
import app.bottlenote.review.dto.request.ReviewImageInfo;
import app.bottlenote.review.exception.ReviewException;
import app.bottlenote.review.repository.ReviewImageRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewImageSupportService {

	private final ReviewImageRepository reviewImageRepository;

	private static final int REVIEW_IMAGE_MAX_SIZE = 5;

	public void updateImages(List<ReviewImageInfo> imageList, Review review) {

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

		if (!isValidReviewImageList(reviewImageList)) {
			throw new ReviewException(INVALID_IMAGE_URL_MAX_SIZE);
		}

		review.updateImage(reviewImageList);

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

		if (!isValidReviewImageList(reviewImageList)) {
			throw new ReviewException(INVALID_IMAGE_URL_MAX_SIZE);
		}
		reviewImageRepository.saveAll(reviewImageList);
	}

	private boolean isValidReviewImageList(List<ReviewImage> reviewImageList) {
		return reviewImageList.size() <= REVIEW_IMAGE_MAX_SIZE;
	}

}
