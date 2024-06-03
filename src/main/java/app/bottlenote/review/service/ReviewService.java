package app.bottlenote.review.service;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_NOT_FOUND;
import static app.bottlenote.user.exception.UserExceptionCode.USER_NOT_FOUND;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.alcohols.repository.AlcoholQueryRepository;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewImage;
import app.bottlenote.review.domain.ReviewTastingTag;
import app.bottlenote.review.dto.request.PageableRequest;
import app.bottlenote.review.dto.request.ReviewCreateRequest;
import app.bottlenote.review.dto.response.ReviewCreateResponse;
import app.bottlenote.review.dto.response.ReviewResponse;
import app.bottlenote.review.repository.ReviewImageRepository;
import app.bottlenote.review.repository.ReviewRepository;
import app.bottlenote.review.repository.ReviewTastingTagRepository;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.repository.UserCommandRepository;
import java.util.List;
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

	@Transactional
	public ReviewCreateResponse createReviews(ReviewCreateRequest reviewCreateRequest, Long currentUserId) {

		//DB에서 Alcohol 엔티티 조회
		Alcohol alcohol = alcoholQueryRepository.findById(reviewCreateRequest.alcoholId())
			.orElseThrow(() -> new AlcoholException(ALCOHOL_NOT_FOUND));

		//현재 로그인 한 user id로 DB에서 User 엔티티 조회
		User user = userCommandRepository.findById(currentUserId).
			orElseThrow(() -> new UserException(USER_NOT_FOUND));

		Review review = Review.builder()
			.alcohol(alcohol)
			.user(user)
			.price(reviewCreateRequest.price())
			.sizeType(reviewCreateRequest.sizeType())
			.status(reviewCreateRequest.status())
			.imageUrl(reviewCreateRequest.imageUrlList().get(0).viewUrl())
			.content(reviewCreateRequest.content())
			.address(reviewCreateRequest.locationInfo().address())
			.zipCode(reviewCreateRequest.locationInfo().zipCode())
			.detailAddress(reviewCreateRequest.locationInfo().detailAddress())
			.build();

		List<ReviewImage> reviewImageList = reviewCreateRequest.imageUrlList().stream()
			.map(image -> ReviewImage.builder()
				.imageUrl(image.viewUrl())
				.review(review)
				.order(image.order())
				.build()
			).toList();

		List<ReviewTastingTag> reviewTastingTagList = reviewCreateRequest.tastingTagList().stream()
			.map(tastingTag -> ReviewTastingTag.builder()
				.review(review)
				.tastingTag(tastingTag)
				.build())
			.toList();

		Review saveReview = reviewRepository.save(review);

		reviewImageRepository.saveAll(reviewImageList);

		reviewTastingTagRepository.saveAll(reviewTastingTagList);

		return ReviewCreateResponse.builder()
			.id(saveReview.getId())
			.content(saveReview.getContent())
			.callback(String.valueOf(saveReview.getAlcohol().getId()))
			.build();
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
}
