package app.bottlenote.review.service;

import static app.bottlenote.shared.review.constant.ReviewActiveStatus.DELETED;
import static app.bottlenote.shared.review.constant.ReviewDisplayStatus.PUBLIC;
import static app.bottlenote.shared.review.constant.ReviewResultMessage.MODIFY_SUCCESS;
import static app.bottlenote.shared.review.constant.ReviewResultMessage.PRIVATE_SUCCESS;
import static app.bottlenote.shared.review.constant.ReviewResultMessage.PUBLIC_SUCCESS;
import static app.bottlenote.shared.review.exception.ReviewExceptionCode.REVIEW_NOT_FOUND;

import app.bottlenote.alcohols.facade.AlcoholFacade;
import app.bottlenote.alcohols.facade.payload.AlcoholSummaryItem;
import app.bottlenote.history.event.publisher.HistoryEventPublisher;
import app.bottlenote.rating.domain.RatingPoint;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewLocation;
import app.bottlenote.review.domain.ReviewRepository;
import app.bottlenote.review.dto.request.ReviewCreateRequest;
import app.bottlenote.review.dto.request.ReviewImageInfoRequest;
import app.bottlenote.review.dto.request.ReviewModifyRequestWrapperItem;
import app.bottlenote.review.dto.request.ReviewPageableRequest;
import app.bottlenote.review.dto.request.ReviewStatusChangeRequest;
import app.bottlenote.review.dto.response.ReviewCreateResponse;
import app.bottlenote.review.dto.response.ReviewDetailResponse;
import app.bottlenote.review.dto.response.ReviewListResponse;
import app.bottlenote.review.dto.response.ReviewResultResponse;
import app.bottlenote.review.event.payload.ReviewRegistryEvent;
import app.bottlenote.shared.cursor.PageResponse;
import app.bottlenote.shared.review.constant.ReviewResultMessage;
import app.bottlenote.shared.review.exception.ReviewException;
import app.bottlenote.shared.review.payload.ReviewInfo;
import app.bottlenote.user.facade.UserFacade;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

  private final AlcoholFacade alcoholFacade;
  private final UserFacade userDomainSupport;
  private final ReviewRepository reviewRepository;
  private final HistoryEventPublisher reviewEventPublisher;

  /** Read */
  @Transactional(readOnly = true)
  public PageResponse<ReviewListResponse> getReviews(
      Long alcoholId, ReviewPageableRequest reviewPageableRequest, Long userId) {
    return reviewRepository.getReviews(alcoholId, reviewPageableRequest, userId);
  }

  @Transactional(readOnly = true)
  public ReviewDetailResponse getDetailReview(Long reviewId, Long currentUserId) {
    Review review =
        reviewRepository
            .findById(reviewId)
            .orElseThrow(() -> new ReviewException(REVIEW_NOT_FOUND));
    AlcoholSummaryItem alcoholSummaryItem =
        alcoholFacade
            .findAlcoholInfoById(review.getAlcoholId(), currentUserId)
            .orElseGet(AlcoholSummaryItem::empty);
    ReviewInfo reviewInfo = reviewRepository.getReview(reviewId, currentUserId);
    return ReviewDetailResponse.create(
        alcoholSummaryItem, reviewInfo, review.getReviewImages().getViewInfo());
  }

  @Transactional(readOnly = true)
  public PageResponse<ReviewListResponse> getMyReviews(
      ReviewPageableRequest reviewPageableRequest, Long alcoholId, Long userId) {
    return reviewRepository.getReviewsByMe(alcoholId, reviewPageableRequest, userId);
  }

  /** Create , Update, Delete */
  @Transactional
  public ReviewCreateResponse createReview(
      ReviewCreateRequest reviewCreateRequest, Long currentUserId) {
    alcoholFacade.isValidAlcoholId(reviewCreateRequest.alcoholId());
    userDomainSupport.isValidUserId(currentUserId);

    RatingPoint point = RatingPoint.of(reviewCreateRequest.rating());
    Review review =
        Review.builder()
            .alcoholId(reviewCreateRequest.alcoholId())
            .userId(currentUserId)
            .reviewRating(point.getRating())
            .price(reviewCreateRequest.price())
            .sizeType(reviewCreateRequest.sizeType())
            .status(reviewCreateRequest.status())
            .content(reviewCreateRequest.content())
            .reviewLocation(
                ReviewLocation.builder()
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
    review.updateTastingTags(reviewCreateRequest.tastingTagList());

    Review saveReview = reviewRepository.save(review);

    ReviewRegistryEvent event =
        ReviewRegistryEvent.of(
            saveReview.getId(),
            saveReview.getAlcoholId(),
            saveReview.getUserId(),
            saveReview.getContent());
    reviewEventPublisher.publishReviewHistoryEvent(event);

    return ReviewCreateResponse.builder()
        .id(saveReview.getId())
        .content(saveReview.getContent())
        .callback(String.valueOf(saveReview.getAlcoholId()))
        .build();
  }

  @Transactional
  public ReviewResultResponse modifyReview(
      final app.bottlenote.review.dto.request.ReviewModifyRequest request,
      final Long reviewId,
      final Long currentUserId) {
    Review review =
        reviewRepository
            .findByIdAndUserId(reviewId, currentUserId)
            .orElseThrow(() -> new ReviewException(REVIEW_NOT_FOUND));

    ReviewModifyRequestWrapperItem reviewModifyRequestWrapperItem =
        ReviewModifyRequestWrapperItem.create(request);
    List<ReviewImageInfoRequest> reviewImageInfoRequests = request.imageUrlList();

    review.update(reviewModifyRequestWrapperItem);
    review.imageInitialization(reviewImageInfoRequests);
    review.updateTastingTags(request.tastingTagList());
    return ReviewResultResponse.response(MODIFY_SUCCESS, reviewId);
  }

  @Transactional
  public ReviewResultResponse deleteReview(Long reviewId, Long currentUserId) {

    Review review =
        reviewRepository
            .findByIdAndUserId(reviewId, currentUserId)
            .orElseThrow(() -> new ReviewException(REVIEW_NOT_FOUND));
    ReviewResultMessage reviewResultMessage = review.updateReviewActiveStatus(DELETED);

    return ReviewResultResponse.response(reviewResultMessage, reviewId);
  }

  @Transactional
  public ReviewResultResponse changeStatus(
      Long reviewId, ReviewStatusChangeRequest reviewDisplayStatus, Long currentUserId) {

    Review review =
        reviewRepository
            .findByIdAndUserId(reviewId, currentUserId)
            .orElseThrow(() -> new ReviewException(REVIEW_NOT_FOUND));

    review.updateDisplayStatus(reviewDisplayStatus.status());

    return review.getStatus() == PUBLIC
        ? ReviewResultResponse.response(PUBLIC_SUCCESS, review.getId())
        : ReviewResultResponse.response(PRIVATE_SUCCESS, review.getId());
  }
}
