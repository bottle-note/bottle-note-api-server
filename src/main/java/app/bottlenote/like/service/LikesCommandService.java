package app.bottlenote.like.service;

import app.bottlenote.like.domain.LikeStatus;
import app.bottlenote.like.domain.LikeUserInfo;
import app.bottlenote.like.domain.Likes;
import app.bottlenote.like.domain.LikesRepository;
import app.bottlenote.like.dto.response.LikesUpdateResponse;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewRepository;
import app.bottlenote.review.exception.ReviewException;
import app.bottlenote.review.exception.ReviewExceptionCode;
import app.bottlenote.user.dto.response.UserProfileInfo;
import app.bottlenote.user.service.UserFacade;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LikesCommandService {

	private final UserFacade userDomainSupport;
	private final ReviewRepository reviewRepository;
	private final LikesRepository likesRepository;


	@Transactional
	public LikesUpdateResponse updateLikes(
		Long userId,
		Long reviewId,
		LikeStatus status
	) {
		Likes likes = likesRepository.findByReviewIdAndUserId(reviewId, userId)
			.orElseGet(() -> {
				Review review = reviewRepository.findById(reviewId)
					.orElseThrow(() -> new ReviewException(ReviewExceptionCode.REVIEW_NOT_FOUND));

				UserProfileInfo userProfileInfo = userDomainSupport.getUserProfileInfo(userId);
				LikeUserInfo userInfo = LikeUserInfo.create(userProfileInfo.id(), userProfileInfo.nickname());

				return Likes.builder()
					.review(review)
					.userInfo(userInfo)
					.status(status)
					.build();
			});

		likes.updateStatus(status);
		likesRepository.save(likes);

		return LikesUpdateResponse.of(
			likes.getId(),
			reviewId,
			likes.getUserInfo().getUserId(),
			likes.getUserInfo().getUserNickName(),
			likes.getStatus()
		);
	}
}
