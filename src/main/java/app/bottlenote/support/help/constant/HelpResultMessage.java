package app.bottlenote.support.help.constant;

import lombok.Getter;

@Getter
public enum HelpResultMessage {

	//수정, 삭제 추가 예정
	REGISTER_SUCCESS("문의글 등록이 성공적으로 완료되었습니다"),
	MODIFY_SUCCESS("문의글 수정이 성공적으로 완료되었습니다"),
	DELETE_SUCCESS("문의글 삭제가 성공적으로 완료되었습니다");

	private final String description;

	HelpResultMessage(String description) {
		this.description = description;
	}
}
