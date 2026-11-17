package app.bottlenote.user.dto.dsl;

import java.time.LocalDateTime;

public record FollowPageableCriteria(int size, LocalDateTime lastModifyAt, Long lastId) {
  public static FollowPageableCriteria first(int size) {
    return new FollowPageableCriteria(size, null, null);
  }

  public boolean hasCursor() {
    return lastId != null && lastModifyAt != null;
  }
}
