package app.bottlenote.review.service;

import app.bottlenote.alcohols.dto.response.AlcoholInfo;
import app.bottlenote.alcohols.service.domain.AlcoholFacade;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.history.event.publisher.HistoryEventPublisher;
import app.bottlenote.rating.domain.RatingPoint;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewLocation;
import app.bottlenote.review.domain.ReviewRepository;
import app.bottlenote.review.dto.payload.ReviewRegistryEvent;
import app.bottlenote.review.dto.request.ReviewCreateRequest;
import app.bottlenote.review.dto.request.ReviewImageInfo;
import app.bottlenote.review.dto.request.ReviewModifyRequest;
import app.bottlenote.review.dto.request.ReviewPageableRequest;
import app.bottlenote.review.dto.request.ReviewStatusChangeRequest;
import app.bottlenote.review.dto.response.ReviewCreateResponse;
import app.bottlenote.review.dto.response.ReviewDetailResponse;
import app.bottlenote.review.dto.response.ReviewListResponse;
import app.bottlenote.review.dto.response.ReviewResultResponse;
import app.bottlenote.review.dto.response.constant.ReviewResultMessage;
import app.bottlenote.review.dto.vo.ReviewInfo;
import app.bottlenote.review.dto.vo.ReviewModifyVO;
import app.bottlenote.review.exception.ReviewException;
import app.bottlenote.user.service.UserFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static app.bottlenote.review.domain.constant.ReviewActiveStatus.DELETED;
import static app.bottlenote.review.domain.constant.ReviewDisplayStatus.PUBLIC;
import static app.bottlenote.review.dto.response.constant.ReviewResultMessage.MODIFY_SUCCESS;
import static app.bottlenote.review.dto.response.constant.ReviewResultMessage.PRIVATE_SUCCESS;
import static app.bottlenote.review.dto.response.constant.ReviewResultMessage.PUBLIC_SUCCESS;
import static app.bottlenote.review.exception.ReviewExceptionCode.REVIEW_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService implements ReviewFacade {

	private final AlcoholFacade alcoholFacade;
	private final UserFacade userDomainSupport;
	private final ReviewRepository reviewRepository;
	private final HistoryEventPublisher reviewEventPublisher;

	/**
	 * Read
	 */
	@Transactional(readOnly = true)
	public PageResponse<ReviewListResponse> getReviews(
		Long alcoholId,
		ReviewPageableRequest reviewPageableRequest,
		Long userId
	) {
		return reviewRepository.getReviews(alcoholId, reviewPageableRequest, userId);
	}

	@Transactional(readOnly = true)
	public ReviewDetailResponse getDetailReview(Long reviewId, Long currentUserId) {
		Review review = reviewRepository.findById(reviewId).orElseThrow(() -> new ReviewException(REVIEW_NOT_FOUND));
		AlcoholInfo alcoholInfo = alcoholFacade.findAlcoholInfoById(review.getAlcoholId(), currentUserId).orElseGet(AlcoholInfo::empty);
		ReviewInfo reviewInfo = reviewRepository.getReview(reviewId, currentUserId);
		return ReviewDetailResponse.create(
			alcoholInfo,
			reviewInfo,
			review.getReviewImages().getViewInfo()
		);
	}

	@Transactional(readOnly = true)
	public PageResponse<ReviewListResponse> getMyReviews(
		ReviewPageableRequest reviewPageableRequest,
		Long alcoholId,
		Long userId) {
		return reviewRepository.getReviewsByMe(alcoholId, reviewPageableRequest, userId);
	}

	@Override
	@Transactional(readOnly = true)
	public ReviewListResponse getReviewInfoList(Long alcoholId, Long userId) {
		ReviewPageableRequest pageableRequest = ReviewPageableRequest.builder().cursor(0L).pageSize(6L).build();
		PageResponse<ReviewListResponse> reviews = reviewRepository.getReviews(alcoholId, pageableRequest, userId);
		return reviews.content();
	}

	@Override
	public boolean isExistReview(Long reviewId) {
		return reviewRepository.existsById(reviewId);
	}

	@Override
	public void requestBlockReview(Long reviewId) {
		reviewRepository.findById(reviewId)
			.ifPresent(Review::blockReview);
	}

	@Override
	public Long getAlcoholIdByReviewId(Long reviewId) {
		return reviewRepository.findById(reviewId)
			.orElseThrow(() -> new ReviewException(REVIEW_NOT_FOUND)).getAlcoholId();
	}

	/**
	 * Create , Update, Delete
	 */

	@Transactional
	public ReviewCreateResponse createReview(
		ReviewCreateRequest reviewCreateRequest,
		Long currentUserId
	) {
		alcoholFacade.isValidAlcoholId(reviewCreateRequest.alcoholId());
		userDomainSupport.isValidUserId(currentUserId);

		RatingPoint point = RatingPoint.of(reviewCreateRequest.rating());
		Review review = Review.builder()
			.alcoholId(reviewCreateRequest.alcoholId())
			.userId(currentUserId)
			.reviewRating(point.getRating())
			.price(reviewCreateRequest.price())
			.sizeType(reviewCreateRequest.sizeType())
			.status(reviewCreateRequest.status())
			.content(reviewCreateRequest.content())
			.reviewLocation(ReviewLocation.builder()
				.name(reviewCreateRequest.locationInfo().locationName())
				.zipCode(reviewCreateRequest.locationInfo().zipCode())
				.address(reviewCreateRequest.locationInfo().address())
				.detailAddress(reviewCreateRequest.locationInfo().detailAddress())
				.category(reviewCreateRequest.locationInfo().category())
				.mapUrl(reviewCreateRequest.locationInfo().mapUrl())
				.latitude(reviewCreateRequest.locationInfo().latitude())
				.longitude(reviewCreateRequest.locationInfo().longitude())
				.build())
			.build();

		review.imageInitialization(reviewCreateRequest.imageUrlList());
		review.saveTastingTag(reviewCreateRequest.tastingTagList());

		Review saveReview = reviewRepository.save(review);

		ReviewRegistryEvent event = ReviewRegistryEvent.of(saveReview.getId(), saveReview.getAlcoholId(), saveReview.getUserId(), saveReview.getContent());
		reviewEventPublisher.publishHistoryEvent(event);

		return ReviewCreateResponse.builder()
			.id(saveReview.getId())
			.content(saveReview.getContent())
			.callback(String.valueOf(saveReview.getAlcoholId()))
			.build();
	}

	@Transactional
	public ReviewResultResponse modifyReview(
		final ReviewModifyRequest request,
		final Long reviewId,
		final Long currentUserId
	) {
		Review review = reviewRepository.findByIdAndUserId(reviewId, currentUserId)
			.orElseThrow(() -> new ReviewException(REVIEW_NOT_FOUND));

		ReviewModifyVO reviewModifyVO = ReviewModifyVO.create(request);
		List<ReviewImageInfo> reviewImageInfos = request.imageUrlList();

		review.update(reviewModifyVO);
		review.imageInitialization(reviewImageInfos);
		review.updateTastingTags(request.tastingTagList());
		return ReviewResultResponse.response(MODIFY_SUCCESS, reviewId);
	}

	@Transactional
	public ReviewResultResponse deleteReview(Long reviewId, Long currentUserId) {

		Review review = reviewRepository.findByIdAndUserId(reviewId, currentUserId).orElseThrow(() -> new ReviewException(REVIEW_NOT_FOUND));
		ReviewResultMessage reviewResultMessage = review.updateReviewActiveStatus(DELETED);

		return ReviewResultResponse.response(reviewResultMessage, reviewId);
	}

	@Transactional
	public ReviewResultResponse changeStatus(
		Long reviewId,
		ReviewStatusChangeRequest reviewDisplayStatus,
		Long currentUserId) {

		Review review = reviewRepository.findByIdAndUserId(reviewId, currentUserId).orElseThrow(
			() -> new ReviewException(REVIEW_NOT_FOUND)
		);

		review.updateDisplayStatus(reviewDisplayStatus.status());

		return review.getStatus() == PUBLIC ?
			ReviewResultResponse.response(PUBLIC_SUCCESS, review.getId()) :
			ReviewResultResponse.response(PRIVATE_SUCCESS, review.getId());
	}

}
