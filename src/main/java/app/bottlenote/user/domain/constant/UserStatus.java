package app.bottlenote.user.domain.constant;

import lombok.Getter;

@Getter
public enum UserStatus {

	ACTIVE("활성상태"),
	DELETED("탈퇴상태");

	private final String description;

	UserStatus(String description) {
		this.description = description;
	}
}
