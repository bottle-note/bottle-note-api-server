package app.bottlenote.review.service;

import static app.bottlenote.review.domain.constant.ReviewActiveStatus.DELETED;
import static app.bottlenote.review.dto.response.constant.ReviewResultMessage.MODIFY_SUCCESS;
import static app.bottlenote.review.exception.ReviewExceptionCode.REVIEW_NOT_FOUND;

import app.bottlenote.alcohols.dto.response.AlcoholInfo;
import app.bottlenote.alcohols.service.domain.AlcoholDomainSupport;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewRepository;
import app.bottlenote.review.dto.request.PageableRequest;
import app.bottlenote.review.dto.request.ReviewCreateRequest;
import app.bottlenote.review.dto.request.ReviewImageInfo;
import app.bottlenote.review.dto.request.ReviewModifyRequest;
import app.bottlenote.review.dto.response.ReviewCreateResponse;
import app.bottlenote.review.dto.response.ReviewDetailResponse;
import app.bottlenote.review.dto.response.ReviewDetailResponse.ReviewInfo;
import app.bottlenote.review.dto.response.ReviewListResponse;
import app.bottlenote.review.dto.response.ReviewResultResponse;
import app.bottlenote.review.dto.response.constant.ReviewResultMessage;
import app.bottlenote.review.dto.vo.ReviewModifyVO;
import app.bottlenote.review.exception.ReviewException;
import app.bottlenote.user.service.domain.UserDomainSupport;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

	private final AlcoholDomainSupport alcoholDomainSupport;
	private final UserDomainSupport userDomainSupport;
	private final ReviewRepository reviewRepository;
	private final ReviewTastingTagSupport reviewTastingTagSupport;
	private final ReviewImageSupport reviewImageSupport;

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
			.address(reviewCreateRequest.locationInfo().address())
			.zipCode(reviewCreateRequest.locationInfo().zipCode())
			.detailAddress(reviewCreateRequest.locationInfo().detailAddress())
			.build();

		Review saveReview = reviewRepository.save(review);

		reviewImageSupport.saveImages(reviewCreateRequest.imageUrlList(), review);

		reviewTastingTagSupport.saveReviewTastingTag(reviewCreateRequest.tastingTagList(), review);

		return ReviewCreateResponse.builder()
			.id(saveReview.getId())
			.content(saveReview.getContent())
			.callback(String.valueOf(saveReview.getAlcoholId()))
			.build();
	}

	@Transactional(readOnly = true)
	public PageResponse<ReviewListResponse> getReviews(Long alcoholId, PageableRequest pageableRequest, Long userId) {
		return reviewRepository.getReviews(alcoholId, pageableRequest, userId);
	}

	@Transactional(readOnly = true)
	public ReviewDetailResponse getDetailReview(Long reviewId, Long currentUserId) {

		long start = System.nanoTime();
		Review review = reviewRepository.findById(reviewId)
			.orElseThrow(() -> new ReviewException(REVIEW_NOT_FOUND));

		log.info("리뷰 존재유무 확인 시간 : {}", (System.nanoTime() - start) / 1_000_000 + "ms");

		long start2 = System.nanoTime();
		AlcoholInfo alcoholInfo = alcoholDomainSupport.findAlcoholInfoById(review.getAlcoholId(), currentUserId)
			.orElseGet(AlcoholInfo::empty);

		log.info("알코올 정보 조회 시간 : {}", (System.nanoTime() - start2) / 1_000_000 + "ms");

		long start3 = System.nanoTime();
		ReviewInfo reviewInfo = reviewRepository.getReview(reviewId, currentUserId);

		log.info("리뷰 정보 조회 시간 : {}", (System.nanoTime() - start3) / 1_000_000 + "ms");

		long start4 = System.nanoTime();
		List<ReviewImageInfo> reviewImageInfos = reviewImageSupport.getReviewImageInfo(review.getReviewImages());

		log.info("리뷰 이미지 조회 시간 : {}", (System.nanoTime() - start4) / 1_000_000 + "ms");

		return ReviewDetailResponse.create(alcoholInfo, reviewInfo, reviewImageInfos);
	}

	@Transactional(readOnly = true)
	public PageResponse<ReviewListResponse> getMyReviews(PageableRequest pageableRequest, Long alcoholId, Long userId) {
		return reviewRepository.getReviewsByMe(alcoholId, pageableRequest, userId);
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
}
