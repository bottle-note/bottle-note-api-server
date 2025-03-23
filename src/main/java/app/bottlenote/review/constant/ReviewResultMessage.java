package app.bottlenote.review.constant;

import lombok.Getter;

@Getter
public enum ReviewResultMessage {

	MODIFY_SUCCESS("리뷰 수정이 성공적으로 완료되었습니다."),
	DELETE_SUCCESS("리뷰 삭제가 성공적으로 완료되었습니다."),
	BLOCK_SUCCESS("해당 리뷰가 비활성화 되었습니다."),
	ACTIVE_SUCCESS("해당 리뷰가 활성화 되었습니다."),
	PUBLIC_SUCCESS("해당 리뷰가 공개 처리되었습니다."),
	PRIVATE_SUCCESS("해당 리뷰가 비공개 처리되었습니다.");

	private final String description;

	ReviewResultMessage(String description) {
		this.description = description;
	}
}
