package app.bottlenote.review.constant;

import lombok.Getter;

@Getter
public enum ReviewActiveStatus {

	ACTIVE("활성"),
	DELETED("삭제"),
	DISABLED("비활성");

	private final String description;

	ReviewActiveStatus(String description) {
		this.description = description;
	}
}
