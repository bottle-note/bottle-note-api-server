package app.bottlenote.review.domain.constant;

import lombok.Getter;

@Getter
public enum ReviewResponseMessage {

	MODIFY_SUCCESS("리뷰 수정이 성공적으로 완료되었습니다."),

	DELETE_SUCCESS("리뷰 삭제가 성공적으로 완료되었습니다."),

	BLOCK_SUCCESS("해당 리뷰가 비활성화 되었습니다."),
	ACTIVE_SUCCESS("해당 리뷰 활성화 되었습니다.");

	private final String description;

	ReviewResponseMessage(String description) {
		this.description = description;
	}
}
