package app.bottlenote.review.domain.constant;

public enum ReviewReplyStatus {
	NORMAL("정상적인 댓글입니다."),
	DELETED("삭제된 댓글입니다."),
	BLOCKED("차단된 댓글입니다."),
	HIDDEN("숨김 처리된 댓글입니다.");

	private final String message;

	ReviewReplyStatus(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}
}
