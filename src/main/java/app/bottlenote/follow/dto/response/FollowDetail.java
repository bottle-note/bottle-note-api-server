package app.bottlenote.follow.dto.response;

import app.bottlenote.follow.domain.constant.FollowStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FollowDetail {

	private Long userId; // 팔로워
	private Long followUserId; // 조회대상유저
	private String nickName;
	private String userProfileImage;
	private FollowStatus status; // 조회대상자가 팔로워를 팔로우하는가
	private Long reviewCount;
	private Long ratingCount;

	@Builder
	public FollowDetail(Long userId, Long followUserId, String nickName, String userProfileImage,
						FollowStatus status, Long reviewCount, Long ratingCount) {
		this.userId = userId;
		this.followUserId = followUserId;
		this.nickName = nickName;
		this.userProfileImage = userProfileImage;
		this.status = status;
		this.reviewCount = reviewCount;
		this.ratingCount = ratingCount;
	}
}
