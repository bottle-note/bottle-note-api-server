package app.bottlenote.follow.fixture;

import app.bottlenote.global.pagination.KeysetPageResponse;
import app.bottlenote.global.pagination.KeysetPagination;
import app.bottlenote.user.constant.FollowStatus;
import app.bottlenote.user.dto.response.FollowerSearchResponse;
import app.bottlenote.user.dto.response.FollowingSearchResponse;
import app.bottlenote.user.dto.response.RelationUserItem;
import java.util.List;

public class FollowQueryFixture {

  public KeysetPageResponse<FollowingSearchResponse> getFollowingPageResponse() {
    List<RelationUserItem> followingDetails =
        List.of(
            RelationUserItem.builder()
                .userId(1L)
                .followUserId(1L)
                .followUserNickname("nickName2")
                .userProfileImage("imageUrl2")
                .status(FollowStatus.FOLLOWING)
                .reviewCount(10L)
                .ratingCount(5L)
                .build());
    return KeysetPageResponse.of(
        FollowingSearchResponse.of(followingDetails), new KeysetPagination(false, null));
  }

  public KeysetPageResponse<FollowerSearchResponse> getFollowerPageResponse() {
    List<RelationUserItem> followerDetails =
        List.of(
            RelationUserItem.builder()
                .userId(1L)
                .followUserId(1L)
                .followUserNickname("nickName1")
                .userProfileImage("imageUrl1")
                .status(FollowStatus.FOLLOWING)
                .reviewCount(10L)
                .ratingCount(5L)
                .build());
    return KeysetPageResponse.of(
        FollowerSearchResponse.of(followerDetails), new KeysetPagination(false, null));
  }
}
