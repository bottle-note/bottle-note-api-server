package app.bottlenote.user.dto.response;

import java.util.List;

public record FollowingSearchResponse(Long totalCount, List<RelationUserItem> followingList) {

  public static FollowingSearchResponse of(Long totalCount, List<RelationUserItem> followingList) {
    return new FollowingSearchResponse(totalCount, followingList);
  }
}
