package app.bottlenote.review.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ImageStatus {

	DELETED("삭제"),
	HIDE("숨김"),
	EXPIRED("유효기간 만료");
	private final String status;

}
