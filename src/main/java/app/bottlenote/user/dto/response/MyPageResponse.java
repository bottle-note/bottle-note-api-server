package app.bottlenote.user.dto.response;

import lombok.Builder;


public record MyPageResponse(

	Long userId,
	String nickName,
	String imageUrl,
	Long reviewCount,
	Long pickCount,
	Long ratingCount,
	Long followerCount,
	Long followingCount,
	Boolean isMyPage,
	Boolean isFollow


) {
	@Builder
	public MyPageResponse {
	}
}
