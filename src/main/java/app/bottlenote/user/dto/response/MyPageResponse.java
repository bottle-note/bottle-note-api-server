package app.bottlenote.user.dto.response;

import lombok.Builder;


public record MyPageResponse(

	Long userId,
	String nickName,
	String imageUrl,
	Long reviewCount,
	Long ratingCount,
	Long pickCount,
	Long followerCount,
	Long followingCount,
	Boolean isFollow,
	Boolean isMyPage


) {
	@Builder
	public MyPageResponse {
	}
}
