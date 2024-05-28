package app.bottlenote.review.service;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.ALCOHOL_NOT_FOUND;
import static app.bottlenote.user.exception.UserExceptionCode.USER_NOT_FOUND;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.alcohols.repository.AlcoholQueryRepository;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.rating.domain.Rating;
import app.bottlenote.rating.domain.RatingId;
import app.bottlenote.rating.domain.RatingPoint;
import app.bottlenote.rating.repository.RatingRepository;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.dto.request.PageableRequest;
import app.bottlenote.review.dto.request.ReviewCreateRequest;
import app.bottlenote.review.dto.response.ReviewCreateResponse;
import app.bottlenote.review.dto.response.ReviewResponse;
import app.bottlenote.review.repository.ReviewRepository;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.repository.UserCommandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

	private final ReviewRepository reviewRepository;
	private final AlcoholQueryRepository alcoholQueryRepository;
	private final UserCommandRepository userCommandRepository;
	private final RatingRepository ratingRepository;

	public PageResponse<ReviewResponse> getReviews(
		Long alcoholId,
		PageableRequest pageableRequest,
		Long userId
	) {

		PageResponse<ReviewResponse> reviews = reviewRepository.getReviews(alcoholId,
			pageableRequest, userId);

		log.info("review size is : {}", reviews.content());

		return reviews;
	}

	public ReviewCreateResponse createReviews(ReviewCreateRequest reviewCreateRequest,
		Long currentUserid) {

		//DB에서 Alcohol 엔티티 조회
		Alcohol alcohol = alcoholQueryRepository.findAlcoholById(reviewCreateRequest.alcoholId())
			.orElseThrow(() -> new AlcoholException(ALCOHOL_NOT_FOUND));

		//현재 로그인 한 user id로 DB에서 User 엔티티 조회
		User user = userCommandRepository.findById(currentUserid).orElseThrow(
			() -> new UserException(USER_NOT_FOUND));

		Review review = Review.builder()
			.alcohol(alcohol)
			.user(user)
			.price(reviewCreateRequest.price())
			.sizeType(reviewCreateRequest.sizeType())
			.status(reviewCreateRequest.reviewStatus())
			.imageUrl(reviewCreateRequest.imageUrl())
			.content(reviewCreateRequest.content())
			.address(reviewCreateRequest.locationInfo().address())
			.zipCode(reviewCreateRequest.locationInfo().zipCode())
			.detailAddress(reviewCreateRequest.locationInfo().detailAddress())
			.build();

		RatingId ratingId = RatingId.is(user.getId(), alcohol.getId());
		Rating rating = Rating.builder()
			.id(ratingId)
			.user(user)
			.ratingPoint(RatingPoint.of(reviewCreateRequest.rating()))
			.alcohol(alcohol)
			.build();

		ratingRepository.save(rating);

		Review saveReview = reviewRepository.save(review);
		return new ReviewCreateResponse(saveReview.getId());
	}
}
