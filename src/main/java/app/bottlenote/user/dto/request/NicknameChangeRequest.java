package app.bottlenote.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import static app.bottlenote.global.security.SecurityContextUtil.getCurrentUserId;

public record NicknameChangeRequest (
	@NotNull(message = "유저 아이디가 업습니다.")
	Long userId,

	@NotBlank(message = "닉네임은 2~11자의 한글, 영문, 숫자만 가능합니다.")
	@Pattern(regexp = "^[a-zA-Z가-힣0-9]{2,11}$", message = "닉네임은 2~11자의 한글, 영문, 숫자만 가능합니다.")
	String nickName
){
	public NicknameChangeRequest(Long userId, String nickName){
		this.userId = getCurrentUserId();
		this.nickName = nickName;
	}

}
