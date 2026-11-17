package app.bottlenote.user.dto.response;

import app.bottlenote.user.constant.FollowStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record RelationUserItem(
    Long userId,
    Long followUserId,
    String followUserNickname,
    String userProfileImage,
    FollowStatus status,
    Long reviewCount,
    Long ratingCount,
    @JsonIgnore Long followId,
    @JsonIgnore LocalDateTime lastModifyAt) {
  public RelationUserItem(
      Long userId,
      Long followUserId,
      String followUserNickname,
      String userProfileImage,
      String status,
      Long reviewCount,
      Long ratingCount,
      Long followId,
      LocalDateTime lastModifyAt) {
    this(
        userId,
        followUserId,
        followUserNickname,
        userProfileImage,
        FollowStatus.parsing(status),
        reviewCount,
        ratingCount,
        followId,
        lastModifyAt);
  }
}
