package app.bottlenote.user.dto.constant;

import lombok.Getter;

@Getter
public enum WithdrawUserResultMessage {
	USER_WITHDRAW_SUCCESS("성공적으로 탈퇴가 완료되었습니다"),
	USER_WITHDRAW_FAIL("탈퇴 중 오류가 발생했습니다. 관리자에게 문의하세요");


	private final String message;

	WithdrawUserResultMessage(String message) {
		this.message = message;
	}
}
