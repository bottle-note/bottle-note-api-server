package app.bottlenote.shared.review.constant;

import lombok.Getter;

@Getter
public enum ReviewReplyResultMessage {
  SUCCESS_REGISTER_REPLY("성공적으로 댓글을 등록했습니다."),
  SUCCESS_MODIFY_REPLY("성공적으로 댓글을 수정했습니다."),
  SUCCESS_DELETE_REPLY("성공적으로 댓글을 삭제했습니다."),
  SUCCESS_REPORT_REPLY("성공적으로 댓글을 신고했습니다."),
  SUCCESS_CANCEL_REPORT_REPLY("성공적으로 댓글 신고를 취소했습니다.");

  private final String message;

  ReviewReplyResultMessage(String message) {
    this.message = message;
  }
}
