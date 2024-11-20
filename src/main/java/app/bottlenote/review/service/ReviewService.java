package app.bottlenote.review.service;

import app.bottlenote.alcohols.dto.response.AlcoholInfo;
import app.bottlenote.alcohols.service.domain.AlcoholDomainSupport;
import app.bottlenote.global.service.cursor.PageResponse;
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
import app.bottlenote.review.event.publisher.ReviewEventPublisher;
import app.bottlenote.review.exception.ReviewException;
import app.bottlenote.user.service.domain.UserDomainSupport;
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

	private final AlcoholDomainSupport alcoholDomainSupport;
	private final UserDomainSupport userDomainSupport;
	private final ReviewRepository reviewRepository;
	private final ReviewTastingTagSupport reviewTastingTagSupport;
	private final ReviewImageSupport reviewImageSupport;
	private final ReviewEventPublisher reviewEventPublisher;

	@Transactional
	public ReviewCreateResponse createReview(ReviewCreateRequest reviewCreateRequest, Long currentUserId) {

		//DB에서 Alcohol 엔티티 조회
		alcoholDomainSupport.isValidAlcoholId(reviewCreateRequest.alcoholId());

		//현재 로그인 한 user id로 DB에서 User 엔티티 조회
		userDomainSupport.isValidUserId(currentUserId);

		Review review = Review.builder()
			.alcoholId(reviewCreateRequest.alcoholId())
			.userId(currentUserId)
			.price(reviewCreateRequest.price())
			.sizeType(reviewCreateRequest.sizeType())
			.status(reviewCreateRequest.status())
			.imageUrl(reviewCreateRequest.imageUrlList().isEmpty() ? null : reviewCreateRequest.imageUrlList().get(0).viewUrl())
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

		//DB에 리뷰 저장
		Review saveReview = reviewRepository.save(review);
		//이미지 저장
		reviewImageSupport.saveImages(reviewCreateRequest.imageUrlList(), review);
		//테이스팅 태그 저장
		reviewTastingTagSupport.saveReviewTastingTag(reviewCreateRequest.tastingTagList(), review);

		//이벤트 발행
		reviewEventPublisher.reviewRegistry(
			ReviewRegistryEvent.of(
				saveReview.getId(),
				saveReview.getAlcoholId(),
				saveReview.getUserId(),
				saveReview.getContent()
			));

		return ReviewCreateResponse.builder()
			.id(saveReview.getId())
			.content(saveReview.getContent())
			.callback(String.valueOf(saveReview.getAlcoholId()))
			.build();
	}

	@Transactional(readOnly = true)
	public PageResponse<ReviewListResponse> getReviews(
		Long alcoholId,
		ReviewPageableRequest reviewPageableRequest,
		Long userId) {
		return reviewRepository.getReviews(alcoholId, reviewPageableRequest, userId);
	}

	@Transactional(readOnly = true)
	public ReviewDetailResponse getDetailReview(Long reviewId, Long currentUserId) {

		Review review = reviewRepository.findById(reviewId).orElseThrow(() -> new ReviewException(REVIEW_NOT_FOUND));
		AlcoholInfo alcoholInfo = alcoholDomainSupport.findAlcoholInfoById(review.getAlcoholId(), currentUserId).orElseGet(AlcoholInfo::empty);
		ReviewInfo reviewInfo = reviewRepository.getReview(reviewId, currentUserId);
		List<ReviewImageInfo> reviewImageInfos = reviewImageSupport.getReviewImageInfo(review.getReviewImages());

		return ReviewDetailResponse.create(alcoholInfo, reviewInfo, reviewImageInfos);
	}

	@Transactional(readOnly = true)
	public PageResponse<ReviewListResponse> getMyReviews(ReviewPageableRequest reviewPageableRequest, Long alcoholId, Long userId) {
		return reviewRepository.getReviewsByMe(alcoholId, reviewPageableRequest, userId);
	}

	@Transactional
	public ReviewResultResponse modifyReview(ReviewModifyRequest reviewModifyRequest, Long reviewId, Long currentUserId) {

		Review review = reviewRepository.findByIdAndUserId(reviewId, currentUserId)
			.orElseThrow(() -> new ReviewException(REVIEW_NOT_FOUND));

		ReviewModifyVO reviewModifyVO = new ReviewModifyVO(reviewModifyRequest);

		review.modifyReview(reviewModifyVO);

		reviewImageSupport.updateImages(reviewModifyRequest.imageUrlList(), review);

		reviewTastingTagSupport.updateReviewTastingTags(reviewModifyRequest.tastingTagList(), review);

		return ReviewResultResponse.response(MODIFY_SUCCESS, reviewId);
	}

	@Transactional
	public ReviewResultResponse deleteReview(Long reviewId, Long currentUserId) {

		Review review = reviewRepository.findByIdAndUserId(reviewId, currentUserId).orElseThrow(() -> new ReviewException(REVIEW_NOT_FOUND));
		ReviewResultMessage reviewResultMessage = review.updateReviewActiveStatus(DELETED);

		return ReviewResultResponse.response(reviewResultMessage, reviewId);
	}

	@Transactional
	public ReviewResultResponse changeStatus(Long reviewId, ReviewStatusChangeRequest reviewDisplayStatus, Long currentUserId) {

		Review review = reviewRepository.findByIdAndUserId(reviewId, currentUserId).orElseThrow(
			() -> new ReviewException(REVIEW_NOT_FOUND)
		);

		review.updateDisplayStatus(reviewDisplayStatus.status());

		return review.getStatus() == PUBLIC ?
			ReviewResultResponse.response(PUBLIC_SUCCESS, review.getId()) :
			ReviewResultResponse.response(PRIVATE_SUCCESS, review.getId());
	}

	@Override
	@Transactional(readOnly = true)
	public ReviewListResponse getReviewInfoList(Long alcoholId, Long userId) {
		ReviewPageableRequest pageableRequest = ReviewPageableRequest.builder().cursor(0L).pageSize(6L).build();
		PageResponse<ReviewListResponse> reviews = reviewRepository.getReviews(alcoholId, pageableRequest, userId);
		return reviews.content();
	}
}
