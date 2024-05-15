package app.bottlenote.user.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
public class NicknameChangeResponse {

	private String message;
	private Long userId;
	private String beforeNickname;
	private String changedNickname;



	@Builder
	public NicknameChangeResponse(NicknameChangeResponse.Message message, Long userId, String beforeNickname, String changedNickname) {
		this.message = message.getMessage();
		this.userId = userId;
		this.beforeNickname = beforeNickname;
		this.changedNickname = changedNickname;
	}



	@AllArgsConstructor
	@Getter
	public enum Message {
		SUCCESS("닉네임이 성공적으로 변경되었습니다."),
		FAIL("닉네임 변경에 실패하였습니다.");
		private final String message;
	}
}
