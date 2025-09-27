package app.bottlenote.picks.event.payload;

import app.bottlenote.shared.picks.constant.PicksStatus;

public record PicksRegistryEvent(Long alcoholId, Long userId, PicksStatus picksStatus) {

  public static PicksRegistryEvent of(Long alcoholId, Long userId, PicksStatus picksStatus) {
    return new PicksRegistryEvent(alcoholId, userId, picksStatus);
  }
}
