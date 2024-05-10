package app.bottlenote.user.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public class NicknameChangeResponse {

	private final String message;

	@JsonProperty("userId")
	private Long userId;

	@JsonProperty("beforeNickname")
	private String beforeNickname;

	@JsonProperty("changedNickname")
	private String changedNickname;

	protected NicknameChangeResponse(String message, Long userId, String beforeNickname, String changedNickname) {
		this.message = message;
		this.userId = userId;
		this.beforeNickname = beforeNickname;
		this.changedNickname = changedNickname;
	}


	public static NicknameChangeResponse of(
		NicknameChangeResponseEnum message
		, Long userId
		, String beforeNickname
		, String changedNickname) {
		return new NicknameChangeResponse(message.getMessage(), userId, beforeNickname, changedNickname);
	}

	@AllArgsConstructor
	@Getter
	public enum NicknameChangeResponseEnum {
		SUCCESS("닉네임이 성공적으로 변경되었습니다."),
		FAIL("닉네임 변경에 실패하였습니다.");
		private final String message;
	}
}
