package app.bottlenote.follow.fixture;

import app.bottlenote.shared.cursor.CursorPageable;
import app.bottlenote.shared.cursor.PageResponse;
import app.bottlenote.user.constant.FollowStatus;
import app.bottlenote.user.dto.response.FollowerSearchResponse;
import app.bottlenote.user.dto.response.FollowingSearchResponse;
import app.bottlenote.user.dto.response.RelationUserItem;
import java.util.List;

public class FollowQueryFixture {

  public PageResponse<FollowingSearchResponse> getFollowingPageResponse() {
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
    FollowingSearchResponse followSearchResponse = FollowingSearchResponse.of(5L, followingDetails);

    return PageResponse.of(
        followSearchResponse,
        CursorPageable.builder().cursor(0L).pageSize(50L).hasNext(false).build());
  }

  public PageResponse<FollowerSearchResponse> getFollowerPageResponse() {
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
    FollowerSearchResponse followerSearchResponse = FollowerSearchResponse.of(5L, followerDetails);

    return PageResponse.of(
        followerSearchResponse,
        CursorPageable.builder().cursor(0L).pageSize(50L).hasNext(false).build());
  }
}
