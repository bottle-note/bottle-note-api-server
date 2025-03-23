package app.bottlenote.like.service;

import app.bottlenote.history.event.publisher.HistoryEventPublisher;
import app.bottlenote.like.domain.LikeStatus;
import app.bottlenote.like.domain.LikeUserInfo;
import app.bottlenote.like.domain.Likes;
import app.bottlenote.like.domain.LikesRepository;
import app.bottlenote.like.dto.response.LikesUpdateResponse;
import app.bottlenote.like.event.payload.LikesRegistryEvent;
import app.bottlenote.review.exception.ReviewException;
import app.bottlenote.review.service.ReviewFacade;
import app.bottlenote.user.dto.response.UserProfileItem;
import app.bottlenote.user.service.UserFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static app.bottlenote.review.exception.ReviewExceptionCode.REVIEW_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class LikesCommandService {

	private final UserFacade userFacade;
	private final ReviewFacade reviewFacade;
	private final LikesRepository likesRepository;
	private final HistoryEventPublisher likesEventPublisher;

	@Transactional
	public LikesUpdateResponse updateLikes(
		Long userId,
		Long reviewId,
		LikeStatus status
	) {
		Likes likes = likesRepository.findByReviewIdAndUserId(reviewId, userId)
			.orElseGet(() -> {

				if (!reviewFacade.isExistReview(reviewId)) {
					throw new ReviewException(REVIEW_NOT_FOUND);
				}
				UserProfileItem userProfileItem = userFacade.getUserProfileInfo(userId);
				LikeUserInfo userInfo = LikeUserInfo.create(userProfileItem.id(), userProfileItem.nickname());

				return Likes.builder()
					.reviewId(reviewId)
					.userInfo(userInfo)
					.status(status)
					.build();
			});

		likes.updateStatus(status);
		likesRepository.save(likes);

		likesEventPublisher.publishHistoryEvent(
			LikesRegistryEvent.of(likes.getReviewId(), likes.getUserInfo().getUserId())
		);

		return LikesUpdateResponse.of(
			likes.getId(),
			reviewId,
			likes.getUserInfo().getUserId(),
			likes.getUserInfo().getUserNickName(),
			likes.getStatus()
		);
	}
}
