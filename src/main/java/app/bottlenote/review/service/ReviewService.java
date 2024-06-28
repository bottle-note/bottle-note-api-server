package app.bottlenote.review.service;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_NOT_FOUND;
import static app.bottlenote.review.domain.constant.ReviewActiveStatus.DELETED;
import static app.bottlenote.review.dto.response.ReviewResultMessage.MODIFY_SUCCESS;
import static app.bottlenote.review.exception.ReviewExceptionCode.REVIEW_NOT_FOUND;
import static app.bottlenote.user.exception.UserExceptionCode.USER_NOT_FOUND;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.AlcoholQueryRepository;
import app.bottlenote.alcohols.dto.response.detail.AlcoholDetailInfo;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.dto.request.PageableRequest;
import app.bottlenote.review.dto.request.ReviewCreateRequest;
import app.bottlenote.review.dto.request.ReviewImageInfo;
import app.bottlenote.review.dto.request.ReviewModifyRequest;
import app.bottlenote.review.dto.response.AlcoholInfo;
import app.bottlenote.review.dto.response.ReviewCreateResponse;
import app.bottlenote.review.dto.response.ReviewDetailResponse;
import app.bottlenote.review.dto.response.ReviewListResponse;
import app.bottlenote.review.dto.response.ReviewReplyInfo;
import app.bottlenote.review.dto.response.ReviewResultMessage;
import app.bottlenote.review.dto.response.ReviewResultResponse;
import app.bottlenote.review.dto.vo.ReviewModifyVO;
import app.bottlenote.review.exception.ReviewException;
import app.bottlenote.review.repository.ReviewRepository;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.repository.UserCommandRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

	private final AlcoholQueryRepository alcoholQueryRepository;
	private final UserCommandRepository userCommandRepository;
	private final ReviewRepository reviewRepository;
	private final ReviewTastingTagSupport reviewTastingTagSupport;
	private final ReviewImageSupport reviewImageSupport;

	@Transactional
	public ReviewCreateResponse createReview(ReviewCreateRequest reviewCreateRequest, Long currentUserId) {

		//DB에서 Alcohol 엔티티 조회
		Alcohol alcohol = alcoholQueryRepository.findById(reviewCreateRequest.alcoholId())
			.orElseThrow(() -> new AlcoholException(ALCOHOL_NOT_FOUND));

		//현재 로그인 한 user id로 DB에서 User 엔티티 조회
		User user = userCommandRepository.findById(currentUserId)
			.orElseThrow(() -> new UserException(USER_NOT_FOUND));

		Review review = Review.builder()
			.alcoholId(alcohol.getId())
			.userId(user.getId())
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
	public PageResponse<ReviewListResponse> getReviews(
		Long alcoholId,
		PageableRequest pageableRequest,
		Long userId
	) {
		return reviewRepository.getReviews(alcoholId, pageableRequest, userId);
	}

	@Transactional(readOnly = true)
	public ReviewDetailResponse getDetailReview(Long reviewId, Long currentUserId) {

		Review review = reviewRepository.findById(reviewId).orElseThrow(
			() -> new ReviewException(REVIEW_NOT_FOUND)
		);

		AlcoholDetailInfo alcoholDetailById = alcoholQueryRepository.findAlcoholDetailById(review.getAlcoholId(), currentUserId);

		AlcoholInfo alcoholInfo = AlcoholInfo.builder()
			.alcoholId(alcoholDetailById.getAlcoholId())
			.engName(alcoholDetailById.getEngName())
			.korName(alcoholDetailById.getKorName())
			.engCategoryName(alcoholDetailById.getEngCategory())
			.korCategoryName(alcoholDetailById.getKorCategory())
			.imageUrl(alcoholDetailById.getAlcoholUrlImg())
			.isPicked(alcoholDetailById.getIsPicked())
			.build();

		ReviewDetailResponse reviewDetailResponse = reviewRepository.getReview(reviewId, currentUserId);
		reviewDetailResponse.updateAlcoholInfo(alcoholInfo);

		List<ReviewImageInfo> reviewImageInfos = new ArrayList<>();
		review.getReviewImages().forEach(
			image -> reviewImageInfos.add(ReviewImageInfo.create(image.getOrder(), image.getImageUrl()))
		);

		List<ReviewReplyInfo> reviewReplies = reviewRepository.getReviewReplies(reviewId);

		reviewDetailResponse.updateReviewReplyList(reviewReplies);
		reviewDetailResponse.updateReviewImageList(reviewImageInfos);

		return reviewDetailResponse;
	}

	@Transactional(readOnly = true)
	public PageResponse<ReviewListResponse> getMyReviews(
		Long alcoholId,
		PageableRequest pageableRequest,
		Long userId
	) {
		return reviewRepository.getReviewsByMe(alcoholId, pageableRequest, userId);
	}

	@Transactional
	public String modifyReview(
		ReviewModifyRequest reviewModifyRequest,
		Long reviewId,
		Long currentUserId
	) {

		Review review = reviewRepository.findByIdAndUserId(reviewId, currentUserId).orElseThrow(
			() -> new ReviewException(REVIEW_NOT_FOUND)
		);

		ReviewModifyVO reviewModifyVO = new ReviewModifyVO(reviewModifyRequest);

		review.modifyReview(reviewModifyVO);

		reviewImageSupport.updateImages(reviewModifyRequest.imageUrlList(), review);

		reviewTastingTagSupport.updateReviewTastingTags(reviewModifyRequest.tastingTagList(), review);

		return MODIFY_SUCCESS.getDescription();
	}

	@Transactional
	public ReviewResultResponse deleteReview(Long reviewId, Long currentUserId) {

		Review review = reviewRepository.findByIdAndUserId(reviewId, currentUserId).orElseThrow(
			() -> new ReviewException(REVIEW_NOT_FOUND)
		);
		ReviewResultMessage reviewResultMessage = review.updateReviewActiveStatus(DELETED);

		return ReviewResultResponse.response(reviewResultMessage, reviewId);
	}
}
