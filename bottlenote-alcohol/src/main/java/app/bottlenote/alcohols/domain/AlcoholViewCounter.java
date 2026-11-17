package app.bottlenote.alcohols.domain;

import java.time.LocalDateTime;
import java.util.Map;

public interface AlcoholViewCounter {

  void increment(Long alcoholId);

  Map<Long, Long> findCounts(LocalDateTime bucketAt);
}
