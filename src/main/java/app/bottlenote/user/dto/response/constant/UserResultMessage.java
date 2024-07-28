package app.bottlenote.user.dto.response.constant;

import lombok.Getter;

@Getter
public enum UserResultMessage {
	USER_WITHDRAW_SUCCESS("성공적으로 탈퇴가 완료되었습니다");

	private final String message;

	UserResultMessage(String message) {
		this.message = message;
	}
}
