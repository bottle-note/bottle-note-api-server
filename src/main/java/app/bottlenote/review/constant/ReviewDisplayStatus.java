package app.bottlenote.review.constant;

import lombok.Getter;

@Getter
public enum ReviewDisplayStatus {

	PUBLIC("공개"),
	PRIVATE("비공개");

	private final String description;

	ReviewDisplayStatus(String description) {
		this.description = description;
	}

}
