package app.bottlenote.review.controller;

import static app.bottlenote.user.exception.UserExceptionCode.REQUIRED_USER_ID;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.global.service.meta.MetaService;
import app.bottlenote.review.dto.request.ReviewCreateRequest;
import app.bottlenote.review.dto.request.ReviewModifyRequest;
import app.bottlenote.review.dto.request.ReviewPageableRequest;
import app.bottlenote.review.dto.request.ReviewStatusChangeRequest;
import app.bottlenote.review.dto.response.ReviewListResponse;
import app.bottlenote.review.service.ReviewService;
import app.bottlenote.user.exception.UserException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reviews")
public class ReviewController {

  private final ReviewService reviewService;

  @PostMapping
  public ResponseEntity<?> createReview(
      @RequestBody @Valid ReviewCreateRequest reviewCreateRequest) {
    Long currentUserId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new UserException(REQUIRED_USER_ID));
    return GlobalResponse.ok(reviewService.createReview(reviewCreateRequest, currentUserId));
  }

  @GetMapping("/{alcoholId}")
  public ResponseEntity<?> getReviews(
      @PathVariable Long alcoholId, @ModelAttribute ReviewPageableRequest reviewPageableRequest) {
    Long currentUserId = SecurityContextUtil.getUserIdByContext().orElse(-1L);
    PageResponse<ReviewListResponse> pageResponse =
        reviewService.getReviews(alcoholId, reviewPageableRequest, currentUserId);

    return GlobalResponse.ok(
        pageResponse.content(),
        MetaService.createMetaInfo().add("pageable", pageResponse.cursorPageable()));
  }

  @GetMapping("/detail/{reviewId}")
  public ResponseEntity<?> getDetailReview(@PathVariable Long reviewId) {

    Long currentUserId = SecurityContextUtil.getUserIdByContext().orElse(-1L);

    return GlobalResponse.ok(reviewService.getDetailReview(reviewId, currentUserId));
  }

  @GetMapping("/me/{alcoholId}")
  public ResponseEntity<?> getMyReviews(
      @ModelAttribute ReviewPageableRequest reviewPageableRequest, @PathVariable Long alcoholId) {

    Long currentUserId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new UserException(REQUIRED_USER_ID));

    PageResponse<ReviewListResponse> myReviews =
        reviewService.getMyReviews(reviewPageableRequest, alcoholId, currentUserId);

    return GlobalResponse.ok(
        myReviews.content(),
        MetaService.createMetaInfo().add("pageable", myReviews.cursorPageable()));
  }

  @PatchMapping("/{reviewId}")
  public ResponseEntity<?> modifyReview(
      @RequestBody @Valid ReviewModifyRequest reviewModifyRequest, @PathVariable Long reviewId) {

    Long currentUserId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new UserException(REQUIRED_USER_ID));

    return GlobalResponse.ok(
        reviewService.modifyReview(reviewModifyRequest, reviewId, currentUserId));
  }

  @PatchMapping("/{reviewId}/display")
  public ResponseEntity<?> changeStatus(
      @PathVariable Long reviewId, @Valid @RequestBody ReviewStatusChangeRequest status) {
    Long currentUserId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new UserException(REQUIRED_USER_ID));

    return GlobalResponse.ok(reviewService.changeStatus(reviewId, status, currentUserId));
  }

  @DeleteMapping("/{reviewId}")
  public ResponseEntity<?> deleteReview(@PathVariable Long reviewId) {
    Long currentUserId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new UserException(REQUIRED_USER_ID));

    return GlobalResponse.ok(reviewService.deleteReview(reviewId, currentUserId));
  }
}
