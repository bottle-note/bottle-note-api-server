package app.bottlenote.user.dto.response;

import java.util.List;

public record FollowerSearchResponse(Long totalCount, List<RelationUserItem> followerList) {

  public static FollowerSearchResponse of(Long totalCount, List<RelationUserItem> followerList) {
    return new FollowerSearchResponse(totalCount, followerList);
  }
}
