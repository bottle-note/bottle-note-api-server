package app.bottlenote.user.dto.response;

import java.util.List;

public record FollowingSearchResponse(List<RelationUserItem> followingList) {

  public static FollowingSearchResponse of(List<RelationUserItem> followingList) {
    return new FollowingSearchResponse(followingList);
  }
}
