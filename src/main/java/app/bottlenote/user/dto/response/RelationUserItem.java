package app.bottlenote.user.dto.response;

import app.bottlenote.user.domain.constant.FollowStatus;
import app.bottlenote.user.domain.constant.FollowStatusConverter;
import jakarta.persistence.Convert;
import lombok.Builder;

@Builder
public record RelationUserItem(
		Long userId,
		Long followUserId,
		String followUserNickname,
		String userProfileImage,
		@Convert(converter = FollowStatusConverter.class)
		FollowStatus status,
		Long reviewCount,
		Long ratingCount
) {
}
