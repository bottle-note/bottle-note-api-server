package app.bottlenote.user.dto.response;

import java.util.List;

public record FollowerSearchResponse(List<RelationUserItem> followerList) {

  public static FollowerSearchResponse of(List<RelationUserItem> followerList) {
    return new FollowerSearchResponse(followerList);
  }
}
