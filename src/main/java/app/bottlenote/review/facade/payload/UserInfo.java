package app.bottlenote.review.facade.payload;

import app.bottlenote.common.block.annotation.BlockWord;

public record UserInfo(
		Long userId,
		@BlockWord(value = "차단된 사용자입니다") String nickName,
		String userProfileImage
) {

	public static UserInfo of(Long userId, String nickName, String userProfileImage) {
		return new UserInfo(userId, nickName, userProfileImage);
	}
}
