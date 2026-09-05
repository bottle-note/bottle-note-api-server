package app.bottlenote.like.service;

import static app.bottlenote.review.exception.ReviewExceptionCode.REVIEW_NOT_FOUND;

import app.bottlenote.like.constant.LikeStatus;
import app.bottlenote.like.domain.LikeUserInfo;
import app.bottlenote.like.domain.Likes;
import app.bottlenote.like.domain.LikesRepository;
import app.bottlenote.like.dto.response.LikesUpdateResponse;
import app.bottlenote.like.event.payload.ReviewLikeActivityEvent;
import app.bottlenote.review.exception.ReviewException;
import app.bottlenote.review.facade.ReviewFacade;
import app.bottlenote.review.facade.payload.ReviewInfo;
import app.bottlenote.user.facade.UserFacade;
import app.bottlenote.user.facade.payload.UserProfileItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LikesCommandService {

  private final UserFacade userFacade;
  private final ReviewFacade reviewFacade;
  private final LikesRepository likesRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public LikesUpdateResponse updateLikes(Long userId, Long reviewId, LikeStatus status) {
    Likes likes =
        likesRepository
            .findByReviewIdAndUserId(reviewId, userId)
            .orElseGet(
                () -> {
                  if (!reviewFacade.isExistReview(reviewId)) {
                    throw new ReviewException(REVIEW_NOT_FOUND);
                  }
                  UserProfileItem userProfileItem = userFacade.getUserProfileInfo(userId);
                  LikeUserInfo userInfo =
                      LikeUserInfo.create(userProfileItem.id(), userProfileItem.nickname());

                  return Likes.builder()
                      .reviewId(reviewId)
                      .userInfo(userInfo)
                      .status(status)
                      .build();
                });

    boolean activated =
        status == LikeStatus.LIKE && (likes.getId() == null || likes.getStatus() != LikeStatus.LIKE);
    likes.updateStatus(status);
    likesRepository.save(likes);

    ReviewInfo reviewInfo = reviewFacade.getReviewInfo(likes.getReviewId(), userId);
    eventPublisher.publishEvent(
        new ReviewLikeActivityEvent(
            likes.getId(),
            reviewInfo.reviewId(),
            reviewFacade.getAlcoholIdByReviewId(reviewInfo.reviewId()),
            reviewInfo.userInfo().userId(),
            likes.getUserInfo().getUserId(),
            reviewInfo.reviewContent(),
            activated));

    return LikesUpdateResponse.of(
        likes.getId(),
        reviewId,
        likes.getUserInfo().getUserId(),
        likes.getUserInfo().getUserNickName(),
        likes.getStatus());
  }
}
