package app.bottlenote.user.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public class NicknameChangeResponse {

	private final String message;

	private Long userId;

	private String beforeNickname;

	private String changedNickname;

	public static NicknameChangeResponse of(
		Message message
		, Long userId
		, String beforeNickname
		, String changedNickname) {
		return new NicknameChangeResponse(message.getMessage(), userId, beforeNickname, changedNickname);
	}

	@AllArgsConstructor
	@Getter
	public enum Message {
		SUCCESS("닉네임이 성공적으로 변경되었습니다."),
		FAIL("닉네임 변경에 실패하였습니다.");
		private final String message;
	}
}