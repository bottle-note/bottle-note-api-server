package app.bottlenote.review.event.payload;

/**
 * 리뷰 댓글 등록 후 <b>Notification(SSOT) 생성 전용</b> 도메인 이벤트.
 *
 * <p>히스토리({@link ReviewRegistryEvent} → HistoryEventPublisher)와는 <b>별도 발행 경로</b>다. 현재는 댓글 등록 시점에
 * History 경로와 Notification 경로가 이중 발행된다.
 *
 * <p><b>후속 통합 (bottle-note/workspace#373)</b>: 활동 관측 1회 → 단일 도메인 이벤트 → History 리스너 / Notification
 * 리스너 분기로 파이프라인을 공동화할 예정. 통합 시 이 타입은 제거되거나 단일 활동 이벤트로 흡수된다.
 *
 * @param reviewId 리뷰 식별자
 * @param reviewAuthorId 리뷰 작성자(알림 수신 후보)
 * @param replyUserId 댓글 작성자
 * @param replyId 생성된 댓글 식별자
 * @param content 댓글 내용
 */
public record ReviewReplyNotificationEvent(
    Long reviewId, Long reviewAuthorId, Long replyUserId, Long replyId, String content) {

  public static ReviewReplyNotificationEvent of(
      Long reviewId, Long reviewAuthorId, Long replyUserId, Long replyId, String content) {
    return new ReviewReplyNotificationEvent(
        reviewId, reviewAuthorId, replyUserId, replyId, content);
  }

  /** 리뷰 작성자 본인 댓글 여부 (알림 생략 판단용). */
  public boolean isSelfReply() {
    return reviewAuthorId != null && reviewAuthorId.equals(replyUserId);
  }
}
