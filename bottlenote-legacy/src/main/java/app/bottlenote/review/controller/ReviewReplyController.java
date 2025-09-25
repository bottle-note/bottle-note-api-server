package app.bottlenote.review.controller;

import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.review.service.ReviewReplyService;
import app.bottlenote.shared.data.response.GlobalResponse;
import app.bottlenote.shared.review.dto.request.ReviewReplyRegisterRequest;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/review/reply")
public class ReviewReplyController {

  private final ReviewReplyService reviewReplyService;

  public ReviewReplyController(ReviewReplyService reviewReplyService) {
    this.reviewReplyService = reviewReplyService;
  }

  /**
   * 리뷰를 등록 한다.
   *
   * @param reviewId 리뷰 ID
   * @param request 요청(댓글 내용 String content, Long parentReplyId)
   * @return 결과 메시지
   */
  @PostMapping("/register/{reviewId}")
  public ResponseEntity<?> registerReviewReply(
      @PathVariable Long reviewId, @RequestBody @Valid ReviewReplyRegisterRequest request) {
    Long userId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new UserException(UserExceptionCode.REQUIRED_USER_ID));

    return GlobalResponse.ok(reviewReplyService.registerReviewReply(reviewId, userId, request));
  }

  @DeleteMapping("/{reviewId}/{replyId}")
  public ResponseEntity<?> deleteReviewReply(
      @PathVariable Long reviewId, @PathVariable Long replyId) {
    Long userId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new UserException(UserExceptionCode.REQUIRED_USER_ID));

    return GlobalResponse.ok(reviewReplyService.deleteReviewReply(reviewId, replyId, userId));
  }

  @GetMapping("/{reviewId}")
  public ResponseEntity<?> getReviewReplyList(
      @PathVariable Long reviewId,
      @RequestParam(required = false, defaultValue = "0") Long cursor,
      @RequestParam(required = false, defaultValue = "50") Long pageSize) {
    return GlobalResponse.ok(reviewReplyService.getReviewRootReplays(reviewId, cursor, pageSize));
  }

  @GetMapping("/{reviewId}/sub/{rootReplyId}")
  public ResponseEntity<?> getSubReviewReplies(
      @PathVariable Long reviewId,
      @PathVariable Long rootReplyId,
      @RequestParam(required = false, defaultValue = "0") Long cursor,
      @RequestParam(required = false, defaultValue = "50") Long pageSize) {
    return GlobalResponse.ok(
        reviewReplyService.getSubReviewReplies(reviewId, rootReplyId, cursor, pageSize));
  }
}
