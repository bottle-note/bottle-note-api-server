package app.bottlenote.review.domain.constant;

import lombok.Getter;

@Getter
public enum ReviewResponse {

	MODIFY_SUCCESS("리뷰 수정이 성공적으로 완료되었습니다.");

	private final String description;

	ReviewResponse(String description) {
		this.description = description;
	}
}
