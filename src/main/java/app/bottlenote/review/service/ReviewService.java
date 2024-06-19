package app.bottlenote.review.service;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_NOT_FOUND;
import static app.bottlenote.review.exception.ReviewExceptionCode.INVALID_IMAGE_URL_MAX_SIZE;
import static app.bottlenote.review.exception.ReviewExceptionCode.INVALID_TASTING_TAG_LIST_SIZE;
import static app.bottlenote.review.exception.ReviewExceptionCode.REVIEW_NOT_FOUND;
import static app.bottlenote.user.exception.UserExceptionCode.USER_NOT_FOUND;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.common.image.ImageUtil;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewImage;
import app.bottlenote.review.domain.ReviewTastingTag;
import app.bottlenote.review.dto.request.PageableRequest;
import app.bottlenote.review.dto.request.ReviewCreateRequest;
import app.bottlenote.review.dto.request.ReviewModifyRequest;
import app.bottlenote.review.dto.response.ReviewCreateResponse;
import app.bottlenote.review.dto.response.ReviewDetail;
import app.bottlenote.review.dto.response.ReviewResponse;
import app.bottlenote.review.exception.ReviewException;
import app.bottlenote.review.repository.ReviewImageRepository;
import app.bottlenote.review.repository.ReviewRepository;
import app.bottlenote.review.repository.ReviewTastingTagRepository;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.repository.UserCommandRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

	private final ReviewRepository reviewRepository;
	private final AlcoholQueryRepository alcoholQueryRepository;
	private final UserCommandRepository userCommandRepository;
	private final ReviewImageRepository reviewImageRepository;
	private final ReviewTastingTagRepository reviewTastingTagRepository;

	private static final int TASTING_TAG_MAX_SIZE = 10;
	private static final int REVIEW_IMAGE_MAX_SIZE = 5;

	@Transactional
	public ReviewCreateResponse createReviews(ReviewCreateRequest reviewCreateRequest, Long currentUserId) {

		//DB에서 Alcohol 엔티티 조회
		Alcohol alcohol = alcoholQueryRepository.findById(reviewCreateRequest.alcoholId())
			.orElseThrow(() -> new AlcoholException(ALCOHOL_NOT_FOUND));

		//현재 로그인 한 user id로 DB에서 User 엔티티 조회
		User user = userCommandRepository.findById(currentUserId)
			.orElseThrow(() -> new UserException(USER_NOT_FOUND));

		Review review = Review.builder()
			.alcohol(alcohol)
			.user(user)
			.price(reviewCreateRequest.price())
			.sizeType(reviewCreateRequest.sizeType())
			.status(reviewCreateRequest.status())
			.imageUrl(reviewCreateRequest.imageUrlList().isEmpty() ? null : reviewCreateRequest.imageUrlList().get(0).viewUrl())
			.content(reviewCreateRequest.content())
			.address(reviewCreateRequest.locationInfo().address())
			.zipCode(reviewCreateRequest.locationInfo().zipCode())
			.detailAddress(reviewCreateRequest.locationInfo().detailAddress())
			.build();

		Review saveReview = reviewRepository.save(review);

		if (!reviewCreateRequest.imageUrlList().isEmpty()) {

			List<ReviewImage> reviewImageList = reviewCreateRequest.imageUrlList().stream()
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

		Set<ReviewTastingTag> reviewTastingTags = reviewCreateRequest.tastingTagList().stream()
			.map(tastingTag -> ReviewTastingTag.builder()
				.review(review)
				.tastingTag((tastingTag))
				.build())
			.collect(Collectors.toSet());

		if (!isValidReviewTastingTag(reviewTastingTags)) {
			throw new ReviewException(INVALID_TASTING_TAG_LIST_SIZE);
		}

		reviewTastingTagRepository.saveAll(reviewTastingTags);

		return ReviewCreateResponse.builder()
			.id(saveReview.getId())
			.content(saveReview.getContent())
			.callback(String.valueOf(saveReview.getAlcohol().getId()))
			.build();
	}

	private boolean isValidReviewImageList(List<ReviewImage> reviewImageList) {
		return reviewImageList.size() <= REVIEW_IMAGE_MAX_SIZE;
	}

	private boolean isValidReviewTastingTag(Set<ReviewTastingTag> reviewTastingTags) {
		return reviewTastingTags.size() <= TASTING_TAG_MAX_SIZE;
	}


	@Transactional(readOnly = true)
	public PageResponse<ReviewResponse> getReviews(
		Long alcoholId,
		PageableRequest pageableRequest,
		Long userId
	) {

		PageResponse<ReviewResponse> reviews = reviewRepository.getReviews(
			alcoholId,
			pageableRequest,
			userId);

		log.info("review size is : {}", reviews.content());

		return reviews;
	}

	public PageResponse<ReviewResponse> getMyReview(
		Long alcoholId,
		PageableRequest pageableRequest,
		Long userId) {

		return reviewRepository.getReviewsByMe(alcoholId, pageableRequest, userId);
	}

	@Transactional
	public ReviewDetail modifyReviews(
		ReviewModifyRequest reviewModifyRequest,
		Long reviewId,
		Long currentUserId) {

		Review review = reviewRepository.findByIdAndUserId(reviewId, currentUserId).orElseThrow(
			() -> new ReviewException(REVIEW_NOT_FOUND)
		);

		log.info(reviewModifyRequest.toString());
		review.changeReview(reviewModifyRequest);

		return reviewRepository.getReview(reviewId, currentUserId);
	}
}
