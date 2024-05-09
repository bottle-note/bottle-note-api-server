package app.bottlenote.review.domain.constant;

import lombok.Getter;

@Getter
public enum ReviewStatus {

	PUBLIC("공개"),
	PRIVATE("비공개");

	private final String description;

	ReviewStatus(String description) {
		this.description = description;
	}

}
