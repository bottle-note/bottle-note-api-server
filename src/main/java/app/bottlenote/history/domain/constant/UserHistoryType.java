package app.bottlenote.history.domain.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserHistoryType {

	PICK("찜"),
	REVIEW("리뷰"),
	RATING("별점");

	private final String description;


}
