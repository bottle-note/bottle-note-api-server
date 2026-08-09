package app.bottlenote.support.help.event.payload;

/**
 * 관리자 문의 답변 commit 후 사용자 알림을 생성하기 위한 도메인 이벤트다.
 *
 * <p>문의 식별자와 작성자, 답변 내용을 Notification 도메인에 전달한다.
 *
 * @param helpId 문의 식별자
 * @param helpUserId 문의 작성자 식별자
 * @param content 관리자 답변 내용
 */
public record HelpAnswerNotificationEvent(Long helpId, Long helpUserId, String content) {

  public static HelpAnswerNotificationEvent of(
      Long helpId, Long helpUserId, String content) {
    return new HelpAnswerNotificationEvent(helpId, helpUserId, content);
  }
}
