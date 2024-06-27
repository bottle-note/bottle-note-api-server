package app.bottlenote.review.domain.constant;

import lombok.Getter;

@Getter
public enum ReviewResponseMessage {

	MODIFY_SUCCESS("리뷰 수정이 성공적으로 완료되었습니다."),

	DELETE_SUCCESS("리뷰 삭제가 성공적으로 완료되었습니다."),

	ALREADY_DELETED("이미 삭제된 리뷰입니다");

	private final String description;

	ReviewResponseMessage(String description) {
		this.description = description;
	}
}
