package app.bottlenote.review.event.payload;

/**
 * 리뷰/댓글 활동에 대한 <b>History(유저 활동 기록) 입력 페이로드</b>.
 *
 * <p>{@link app.bottlenote.history.event.publisher.HistoryEventPublisher}가 이를 받아 {@code
 * HistoryEvent}로 변환·발행한다. Notification 경로와는 무관하다.
 *
 * <p>댓글 등록 시 현재 구조:
 *
 * <pre>
 *   ReviewRegistryEvent              → History (user_histories)
 *   ReviewReplyNotificationEvent     → Notification (notifications)  // 별도 발행
 * </pre>
 *
 * <p><b>후속 통합 (bottle-note/workspace#373)</b>: History·Notification 파이프라인 공동화 시 단일 활동 이벤트로 흡수·정리될 수
 * 있다.
 */
public record ReviewRegistryEvent(Long reviewId, Long alcoholId, Long userId, String content) {
  public static ReviewRegistryEvent of(Long id, Long alcoholId, Long userId, String content) {
    return new ReviewRegistryEvent(id, alcoholId, userId, content);
  }
}
